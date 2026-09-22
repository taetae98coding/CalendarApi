package io.github.taetae98coding.calendar.calendar

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

/** 공휴일과 음력을 한 번의 요청으로 받을 수 있도록 합쳐 둔 통합 API 를 생성한다. */
data object CalendarUpdater {
    suspend fun update(config: Config, holidays: Map<Country, Map<Int, List<Holiday>>>) {
        coroutineScope {
            config.years.map { year -> async { updateYear(year, holidays) } }
                .awaitAll()
        }
    }

    private suspend fun updateYear(year: Int, holidays: Map<Country, Map<Int, List<Holiday>>>) {
        val yearLunar = FileDataSource.readOrNull<List<LunarDate>>(Paths.lunarYear(year)).orEmpty()
        val yearMonths = (1..12).map { month -> YearMonth(year, month) }
        val lunarByYearMonth = coroutineScope {
            yearMonths.map { yearMonth ->
                async {
                    val lunar = yearLunar.filter { lunar -> lunar.solar.yearMonth == yearMonth }
                        .ifEmpty { FileDataSource.readOrNull<List<LunarDate>>(Paths.lunarYearMonth(yearMonth)).orEmpty() }

                    yearMonth to lunar
                }
            }
                .awaitAll()
                .toMap()
        }

        coroutineScope {
            Country.entries.forEach { country ->
                val countryHolidays = holidays[country]?.get(year).orEmpty()

                launch {
                    FileDataSource.write(
                        value = CalendarResponse(
                            country = country.code,
                            period = "$year",
                            holidays = countryHolidays,
                            lunar = yearLunar,
                        ),
                        file = Paths.calendarYear(country, year),
                    )
                }

                yearMonths.forEach { yearMonth ->
                    launch {
                        FileDataSource.write(
                            value = CalendarResponse(
                                country = country.code,
                                period = "$yearMonth",
                                holidays = countryHolidays.filter { holiday -> holiday.overlaps(yearMonth) },
                                lunar = lunarByYearMonth.getValue(yearMonth),
                            ),
                            file = Paths.calendarYearMonth(country, yearMonth),
                        )
                    }
                }
            }
        }
    }

    private fun Holiday.overlaps(yearMonth: YearMonth): Boolean {
        return start.yearMonth <= yearMonth && yearMonth <= endInclusive.yearMonth
    }
}
