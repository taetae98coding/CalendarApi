package io.github.taetae98coding.calendar.meta

import io.github.taetae98coding.calendar.Config
import io.github.taetae98coding.calendar.Paths
import io.github.taetae98coding.calendar.file.FileDataSource
import io.github.taetae98coding.calendar.holiday.Country
import kotlin.time.Clock

data object MetaWriter {
    private val sources = mapOf(
        Country.KOREA to listOf(
            "한국천문연구원 특일 정보 (공공데이터포털)",
            "Nager.Date (2004년 이전 및 미고시 연도 보완)",
        ),
        Country.UNITED_STATES to listOf("Nager.Date"),
    )

    suspend fun write(): ApiMeta {
        val holidayYears = Coverage.holidayYears()
        val lunarYears = Coverage.lunarYears()

        val meta = ApiMeta(
            updatedAt = Clock.System.now().toString(),
            holiday = HolidayMeta(
                startYear = holidayYears.values.flatten().minOrNull(),
                endInclusiveYear = holidayYears.values.flatten().maxOrNull(),
                countries = Country.entries.map { country ->
                    val years = holidayYears[country].orEmpty()

                    CountryMeta(
                        code = country.code,
                        name = country.displayName,
                        startYear = years.minOrNull(),
                        endInclusiveYear = years.maxOrNull(),
                        sources = sources.getValue(country),
                    )
                },
            ),
            lunar = LunarMeta(
                startYear = Config.LUNAR_MIN_YEAR,
                endInclusiveYear = Config.LUNAR_MAX_YEAR,
                generated = lunarYears.toYearRanges(),
                missing = (Config.LUNAR_MIN_YEAR..Config.LUNAR_MAX_YEAR).filterNot(lunarYears.toSet()::contains).toYearRanges(),
                source = "한국천문연구원 음양력 정보 (공공데이터포털)",
            ),
        )

        FileDataSource.write(meta, Paths.meta)

        return meta
    }
}
