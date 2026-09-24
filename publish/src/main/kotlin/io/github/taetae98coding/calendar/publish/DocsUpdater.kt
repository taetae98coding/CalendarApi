package io.github.taetae98coding.calendar.publish

import io.github.taetae98coding.calendar.core.file.JsonFiles
import io.github.taetae98coding.calendar.domain.CalendarYears
import io.github.taetae98coding.calendar.domain.Country
import io.github.taetae98coding.calendar.domain.holiday.Holiday
import io.github.taetae98coding.calendar.domain.holiday.HolidayRepository
import io.github.taetae98coding.calendar.domain.lunar.LunarDate
import io.github.taetae98coding.calendar.domain.lunar.LunarRepository
import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth

/**
 * 배포 문서를 다시 만든다. 입력은 오직 저장소다.
 *
 * 이번 실행에서 무엇을 새로 받았는지와 무관하게 [years] 전 구간을 다시 읽어 덮어쓴다.
 * 수집과 배포가 분리되어 있어 수집이 한 건도 안 됐어도 문서는 항상 캐시와 같은 상태가 된다.
 *
 * 자료가 없는 구간도 빈 목록으로 쓴다. 경로가 곧 공개 URL 이라 있다가 없어지면 소비처가 깨진다.
 * 대신 어느 연도에 실제 내용이 있는지를 [Coverage] 로 돌려준다.
 *
 * 저장소를 인터페이스로 받으므로 여기서는 자료가 KASI 에서 왔는지 Nager.Date 에서 왔는지 알지 못한다.
 */
class DocsUpdater(
    private val paths: DocPaths,
    private val holidayRepository: HolidayRepository,
    private val lunarRepository: LunarRepository,
    private val years: IntRange = CalendarYears.years,
) {
    suspend fun update(): Coverage {
        val coverage = Coverage.Builder()
        val semaphore = Semaphore(YEAR_PARALLELISM)

        coroutineScope {
            years.map { year -> async { semaphore.withPermit { updateYear(year, coverage) } } }.awaitAll()
        }

        return coverage.build()
    }

    private suspend fun updateYear(year: Int, coverage: Coverage.Builder) {
        // 음력은 국가와 무관하게 같은 자료라 한 번만 읽어 국가별 경로에 나눠 쓴다.
        val lunar = lunarRepository.get(year)
        val lunarByMonth = lunar.groupBy { lunarDate -> lunarDate.solar.yearMonth }

        coroutineScope {
            Country.entries.forEach { country ->
                launch {
                    val holidays = holidayRepository.get(country, year)
                    coverage.record(country, year, holidays, lunar)

                    launch { write(country, "$year", holidays, lunar) { api -> paths.year(api, country, year) } }

                    (1..12).forEach { month ->
                        val yearMonth = YearMonth(year, month)
                        val monthHolidays = holidays.filter { holiday -> holiday.overlaps(yearMonth) }
                        val monthLunar = lunarByMonth[yearMonth].orEmpty()

                        launch { write(country, "$yearMonth", monthHolidays, monthLunar) { api -> paths.month(api, country, yearMonth) } }
                    }
                }
            }
        }
    }

    /** 구간 하나에 세 API 파일을 쓴다. 연 단위와 월 단위는 경로만 다르다. */
    private suspend fun write(country: Country, period: String, holidays: List<Holiday>, lunar: List<LunarDate>, file: (DocApi) -> File) {
        coroutineScope {
            launch { JsonFiles.pretty.write(holidays, file(DocApi.HOLIDAY)) }
            launch { JsonFiles.pretty.write(lunar, file(DocApi.LUNAR)) }
            launch { JsonFiles.pretty.write(CalendarResponse(country.code, period, holidays, lunar), file(DocApi.CALENDAR)) }
        }
    }

    private fun Coverage.Builder.record(country: Country, year: Int, holidays: List<Holiday>, lunar: List<LunarDate>) {
        if (holidays.isNotEmpty()) add(DocApi.HOLIDAY, country, year)
        if (lunar.isNotEmpty()) add(DocApi.LUNAR, country, year)
        if (holidays.isNotEmpty() || lunar.isNotEmpty()) add(DocApi.CALENDAR, country, year)
    }

    private fun Holiday.overlaps(yearMonth: YearMonth): Boolean = start.yearMonth <= yearMonth && yearMonth <= endInclusive.yearMonth

    companion object {
        /** 연도 하나가 국가별 39개 파일을 만든다. 전 범위를 한꺼번에 띄우지 않고 몇 해씩 진행한다. */
        private const val YEAR_PARALLELISM = 8
    }
}
