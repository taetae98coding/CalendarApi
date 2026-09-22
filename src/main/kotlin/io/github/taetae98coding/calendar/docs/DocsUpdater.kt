package io.github.taetae98coding.calendar.docs

import io.github.taetae98coding.calendar.Config
import io.github.taetae98coding.calendar.Paths
import io.github.taetae98coding.calendar.file.FileDataSource
import io.github.taetae98coding.calendar.holiday.Country
import io.github.taetae98coding.calendar.holiday.Holiday
import io.github.taetae98coding.calendar.lunar.LunarDate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth

/**
 * 배포 문서를 다시 만든다. 입력은 오직 캐시다.
 *
 * 이번 실행에서 무엇을 새로 받았는지와 무관하게 [Config.years] 전 구간을 캐시에서 다시 읽어 덮어쓴다.
 * 수집과 배포가 분리되어 있어 예산이 0이어도 문서는 항상 캐시와 같은 상태가 된다.
 */
data object DocsUpdater {
    suspend fun update() {
        coroutineScope {
            Config.years.map { year -> async { updateYear(year) } }
                .awaitAll()
        }
    }

    private suspend fun updateYear(year: Int) {
        val yearMonths = (1..12).map { month -> YearMonth(year, month) }

        // 음력은 국가와 무관하게 같은 자료라 한 번만 읽어 국가별 경로에 나눠 쓴다.
        val lunar = LunarReader.read(year)
        val lunarByYearMonth = lunar.groupBy { lunarDate -> lunarDate.solar.yearMonth }

        coroutineScope {
            Country.entries.forEach { country ->
                launch {
                    val holidays = HolidayReader.read(country, year)
                    val holidaysByYearMonth = yearMonths.associateWith { yearMonth ->
                        holidays.filter { holiday -> holiday.overlaps(yearMonth) }
                    }

                    coroutineScope {
                        launch { FileDataSource.write(holidays, Paths.docYear(DocApi.HOLIDAY, country, year)) }
                        launch { FileDataSource.write(lunar, Paths.docYear(DocApi.LUNAR, country, year)) }
                        launch {
                            FileDataSource.write(
                                value = CalendarResponse(country.code, "$year", holidays, lunar),
                                file = Paths.docYear(DocApi.CALENDAR, country, year),
                            )
                        }

                        yearMonths.forEach { yearMonth ->
                            val monthHolidays = holidaysByYearMonth.getValue(yearMonth)
                            val monthLunar = lunarByYearMonth[yearMonth].orEmpty()

                            launch { FileDataSource.write(monthHolidays, Paths.docMonth(DocApi.HOLIDAY, country, yearMonth)) }
                            launch { FileDataSource.write(monthLunar, Paths.docMonth(DocApi.LUNAR, country, yearMonth)) }
                            launch {
                                FileDataSource.write(
                                    value = CalendarResponse(country.code, "$yearMonth", monthHolidays, monthLunar),
                                    file = Paths.docMonth(DocApi.CALENDAR, country, yearMonth),
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
