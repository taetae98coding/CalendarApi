package io.github.taetae98coding.calendar.publish

import io.github.taetae98coding.calendar.datasource.file.JsonFiles
import io.github.taetae98coding.calendar.domain.CalendarYears
import io.github.taetae98coding.calendar.domain.Country
import kotlin.time.Clock

/** `docs/meta.json` 을 만든다. 출처 표기는 소비처에 보여 주는 설명이라 여기서 관리한다. */
class MetaWriter(
    private val paths: DocPaths,
    private val clock: Clock = Clock.System,
    private val years: IntRange = CalendarYears.years,
) {
    suspend fun write(coverage: Coverage): ApiMeta {
        val meta = ApiMeta(
            updatedAt = clock.now(),
            startYear = years.first,
            endInclusiveYear = years.last,
            apis = DocApi.entries.map { api -> ApiMeta.Api(api.id, countries(api, coverage)) },
        )

        JsonFiles.pretty.write(meta, paths.meta)

        return meta
    }

    private fun countries(api: DocApi, coverage: Coverage): List<ApiMeta.CountryCoverage> {
        return Country.entries.map { country ->
            ApiMeta.CountryCoverage(
                code = country.code,
                name = country.displayName,
                generated = coverage.ranges(api, country),
                missing = coverage.missing(api, country, years),
                sources = sources(api, country),
            )
        }
    }

    private fun sources(api: DocApi, country: Country): List<String> {
        val holiday = when (country) {
            Country.KOREA -> listOf(KASI_SPCDE, NAGER_FALLBACK)
            Country.UNITED_STATES -> listOf(NAGER)
        }

        return when (api) {
            DocApi.HOLIDAY -> holiday
            DocApi.LUNAR -> listOf(KASI_LUNAR)
            DocApi.CALENDAR -> holiday + KASI_LUNAR
        }
    }

    companion object {
        private const val KASI_SPCDE = "한국천문연구원 특일 정보 (공공데이터포털)"
        private const val KASI_LUNAR = "한국천문연구원 음양력 정보 (공공데이터포털)"
        private const val NAGER = "Nager.Date"
        private const val NAGER_FALLBACK = "Nager.Date (특일 정보에 공휴일이 없는 연도 보완)"
    }
}
