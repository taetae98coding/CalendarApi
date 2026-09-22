package io.github.taetae98coding.calendar.publish

import io.github.taetae98coding.calendar.datasource.file.FileDataSource
import io.github.taetae98coding.calendar.domain.CalendarYears
import io.github.taetae98coding.calendar.domain.calendar.CalendarResponse
import io.github.taetae98coding.calendar.domain.holiday.Country
import io.github.taetae98coding.calendar.domain.holiday.Holiday
import io.github.taetae98coding.calendar.domain.holiday.HolidayRepository
import io.github.taetae98coding.calendar.domain.lunar.LunarRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth

/**
 * 배포 문서를 다시 만든다. 입력은 오직 저장소다.
 *
 * 이번 실행에서 무엇을 새로 받았는지와 무관하게 [CalendarYears.years] 전 구간을 다시 읽어 덮어쓴다.
 * 수집과 배포가 분리되어 있어 예산이 0이어도 문서는 항상 캐시와 같은 상태가 된다.
 *
 * 저장소를 인터페이스로 받으므로 여기서는 자료가 KASI 에서 왔는지 Nager.Date 에서 왔는지 알지 못한다.
 */
data object DocsUpdater {
    suspend fun update(holidayRepository: HolidayRepository, lunarRepository: LunarRepository) {
        coroutineScope {
            CalendarYears.years.map { year -> async { updateYear(holidayRepository, lunarRepository, year) } }
                .awaitAll()
        }
    }

    private suspend fun updateYear(holidayRepository: HolidayRepository, lunarRepository: LunarRepository, year: Int) {
        val yearMonths = (1..12).map { month -> YearMonth(year, month) }

        // 음력은 국가와 무관하게 같은 자료라 한 번만 읽어 국가별 경로에 나눠 쓴다.
        val lunar = lunarRepository.get(year)
        val lunarByYearMonth = lunar.groupBy { lunarDate -> lunarDate.solar.yearMonth }

        coroutineScope {
            Country.entries.forEach { country ->
                launch {
                    val holidays = holidayRepository.get(country, year)
                    val holidaysByYearMonth = yearMonths.associateWith { yearMonth ->
                        holidays.filter { holiday -> holiday.overlaps(yearMonth) }
                    }

                    coroutineScope {
                        launch { FileDataSource.write(holidays, DocPaths.year(DocApi.HOLIDAY, country, year)) }
                        launch { FileDataSource.write(lunar, DocPaths.year(DocApi.LUNAR, country, year)) }
                        launch {
                            FileDataSource.write(
                                value = CalendarResponse(country.code, "$year", holidays, lunar),
                                file = DocPaths.year(DocApi.CALENDAR, country, year),
                            )
                        }

                        yearMonths.forEach { yearMonth ->
                            val monthHolidays = holidaysByYearMonth.getValue(yearMonth)
                            val monthLunar = lunarByYearMonth[yearMonth].orEmpty()

                            launch { FileDataSource.write(monthHolidays, DocPaths.month(DocApi.HOLIDAY, country, yearMonth)) }
                            launch { FileDataSource.write(monthLunar, DocPaths.month(DocApi.LUNAR, country, yearMonth)) }
                            launch {
                                FileDataSource.write(
                                    value = CalendarResponse(country.code, "$yearMonth", monthHolidays, monthLunar),
                                    file = DocPaths.month(DocApi.CALENDAR, country, yearMonth),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Holiday.overlaps(yearMonth: YearMonth): Boolean {
        return start.yearMonth <= yearMonth && yearMonth <= endInclusive.yearMonth
    }
}
