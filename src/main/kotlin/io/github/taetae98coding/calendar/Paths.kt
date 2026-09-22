package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.holiday.Country
import java.io.File
import kotlinx.datetime.YearMonth

/** GitHub Pages 로 배포되는 정적 API 의 파일 경로 규칙. */
data object Paths {
    val docs = File("docs")
    val cache = File("cache")

    fun holidayDirectory(country: Country): File = File(docs, "holiday/${country.code}")

    fun holidayYear(country: Country, year: Int): File = File(docs, "holiday/${country.code}/$year.json")

    fun holidayYearMonth(country: Country, yearMonth: YearMonth): File = File(docs, "holiday/${country.code}/$yearMonth.json")

    val lunarDirectory = File(docs, "lunar")

    fun lunarYear(year: Int): File = File(docs, "lunar/$year.json")

    fun lunarYearMonth(yearMonth: YearMonth): File = File(docs, "lunar/$yearMonth.json")

    fun calendarYear(country: Country, year: Int): File = File(docs, "calendar/${country.code}/$year.json")

    fun calendarYearMonth(country: Country, yearMonth: YearMonth): File = File(docs, "calendar/${country.code}/$yearMonth.json")

    val meta = File(docs, "meta.json")

    fun kasiSpcdeCache(api: String, yearMonth: YearMonth): File = File(cache, "kasi/spcde/$api/$yearMonth.json")

    fun kasiLunarCache(yearMonth: YearMonth): File = File(cache, "kasi/lunar/$yearMonth.json")

    fun nagerCache(country: Country, year: Int): File = File(cache, "nager/${country.code}/$year.json")
}
