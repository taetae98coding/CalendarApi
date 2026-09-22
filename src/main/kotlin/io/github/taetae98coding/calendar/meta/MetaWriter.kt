package io.github.taetae98coding.calendar.meta

import io.github.taetae98coding.calendar.Config
import io.github.taetae98coding.calendar.Paths
import io.github.taetae98coding.calendar.file.FileDataSource
import io.github.taetae98coding.calendar.holiday.Country
import io.github.taetae98coding.calendar.lunar.LunarResult
import kotlin.time.Clock

data object MetaWriter {
    private val sources = mapOf(
        Country.KOREA to listOf(
            "한국천문연구원 특일 정보 (공공데이터포털)",
            "Nager.Date (2004년 이전 및 미고시 연도 보완)",
        ),
        Country.UNITED_STATES to listOf("Nager.Date"),
    )

    suspend fun write(config: Config, lunarResult: LunarResult) {
        val meta = ApiMeta(
            updatedAt = Clock.System.now().toString(),
            holiday = HolidayMeta(
                startYear = config.startYear,
                endInclusiveYear = config.endInclusiveYear,
                countries = Country.entries.map { country ->
                    CountryMeta(
                        code = country.code,
                        name = country.displayName,
                        sources = sources.getValue(country),
                    )
                },
            ),
            lunar = LunarMeta(
                startYear = config.lunarStartYear,
                endInclusiveYear = config.lunarEndInclusiveYear,
                generatedStartYear = lunarResult.completedYears.minOrNull(),
                generatedEndInclusiveYear = lunarResult.completedYears.maxOrNull(),
                missingYears = lunarResult.missingYears,
                source = "한국천문연구원 음양력 정보 (공공데이터포털)",
            ),
        )

        FileDataSource.write(meta, Paths.meta)
    }
}
