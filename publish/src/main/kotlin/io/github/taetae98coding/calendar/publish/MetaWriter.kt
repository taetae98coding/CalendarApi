package io.github.taetae98coding.calendar.publish

import io.github.taetae98coding.calendar.core.file.JsonFiles
import io.github.taetae98coding.calendar.domain.CalendarYears
import io.github.taetae98coding.calendar.domain.Country
import kotlin.time.Clock

/** `docs/meta.json` 을 만든다. 갱신 시각, 고정 범위, 실제 생성된 범위, 출처를 적는다. */
class MetaWriter(
    private val paths: DocPaths,
    private val catalog: SourceCatalog,
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
                sources = catalog.sources(api, country),
            )
        }
    }
}
