package io.github.taetae98coding.calendar.publish

import io.github.taetae98coding.calendar.domain.Country

/**
 * 실제로 내용이 있는 연도. 고정 범위 안이어도 원천에 자료가 없으면 비어 있는 파일이 배포된다.
 *
 * 파일이 있는지가 아니라 항목이 있는지를 센다. 파일은 범위 전체에 언제나 만들어지기 때문이다.
 */
class Coverage(
    private val years: Map<DocApi, Map<Country, Set<Int>>>,
) {
    fun years(api: DocApi, country: Country): List<Int> = years[api]?.get(country).orEmpty().sorted()

    /** 연속된 연도를 구간으로 묶는다. 수십 개까지 늘어날 수 있는 목록을 짧게 표현하기 위함이다. */
    fun ranges(api: DocApi, country: Country): List<ApiMeta.YearRange> = years(api, country).toYearRanges()

    fun missing(api: DocApi, country: Country, range: IntRange): List<ApiMeta.YearRange> {
        val generated = years(api, country).toSet()

        return range.filterNot(generated::contains).toYearRanges()
    }

    class Builder {
        private val years = mutableMapOf<DocApi, MutableMap<Country, MutableSet<Int>>>()

        @Synchronized
        fun add(api: DocApi, country: Country, year: Int) {
            years.getOrPut(api, ::mutableMapOf).getOrPut(country, ::mutableSetOf).add(year)
        }

        @Synchronized
        fun build(): Coverage = Coverage(years.mapValues { (_, byCountry) -> byCountry.mapValues { (_, set) -> set.toSet() } })
    }
}

fun List<Int>.toYearRanges(): List<ApiMeta.YearRange> {
    return sorted().fold(emptyList()) { acc, year ->
        val last = acc.lastOrNull()

        if (last != null && last.endInclusive + 1 == year) {
            acc.dropLast(1) + last.copy(endInclusive = year)
        } else {
            acc + ApiMeta.YearRange(year, year)
        }
    }
}
