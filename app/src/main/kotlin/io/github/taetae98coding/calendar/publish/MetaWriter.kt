package io.github.taetae98coding.calendar.publish

import io.github.taetae98coding.calendar.Config
import io.github.taetae98coding.calendar.datasource.file.FileDataSource
import io.github.taetae98coding.calendar.domain.CalendarYears
import io.github.taetae98coding.calendar.domain.holiday.Country
import io.github.taetae98coding.calendar.publish.DocApi
import kotlin.time.Clock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data object MetaWriter {
    private const val KASI_SPCDE = "한국천문연구원 특일 정보 (공공데이터포털)"
    private const val KASI_LUNAR = "한국천문연구원 음양력 정보 (공공데이터포털)"
    private const val NAGER = "Nager.Date"
    private const val NAGER_FALLBACK = "Nager.Date (특일 정보에 공휴일이 없는 연도 보완)"

    suspend fun write(): ApiMeta {
        val meta = ApiMeta(
            updatedAt = Clock.System.now().toString(),
            startYear = CalendarYears.START_YEAR,
            endInclusiveYear = CalendarYears.END_INCLUSIVE_YEAR,
            apis = coroutineScope {
                DocApi.entries.map { api -> async { ApiCoverage(api.id, countries(api)) } }
                    .awaitAll()
            },
        )

        FileDataSource.write(meta, DocPaths.meta)

        return meta
    }

    private suspend fun countries(api: DocApi): List<CountryCoverage> {
        return coroutineScope {
            Country.entries.map { country ->
                async {
                    val years = Coverage.years(api, country)

                    CountryCoverage(
                        code = country.code,
                        name = country.displayName,
                        generated = years.toYearRanges(),
                        missing = CalendarYears.years.filterNot(years.toSet()::contains).toYearRanges(),
                        sources = sources(api, country),
                    )
                }
            }
                .awaitAll()
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
}
