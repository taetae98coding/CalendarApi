package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.domain.Country
import io.github.taetae98coding.calendar.publish.ApiMeta
import io.github.taetae98coding.calendar.publish.Coverage
import io.github.taetae98coding.calendar.publish.DocApi
import io.github.taetae98coding.calendar.publish.toYearRanges
import kotlin.test.Test
import kotlin.test.assertEquals

class CoverageTest {
    @Test
    fun `연속된 연도를 구간으로 묶는다`() {
        assertEquals(
            listOf(ApiMeta.YearRange(1998, 2000), ApiMeta.YearRange(2002, 2002), ApiMeta.YearRange(2004, 2005)),
            listOf(2005, 1998, 1999, 2004, 2000, 2002).toYearRanges(),
        )
        assertEquals("1998 ~ 2000", ApiMeta.YearRange(1998, 2000).text)
        assertEquals("2002", ApiMeta.YearRange(2002, 2002).text)
    }

    @Test
    fun `없는 연도는 고정 범위에서 있는 연도를 뺀 것이다`() {
        val coverage = Coverage.Builder().apply {
            add(DocApi.HOLIDAY, Country.KOREA, 2025)
            add(DocApi.HOLIDAY, Country.KOREA, 2026)
            add(DocApi.HOLIDAY, Country.KOREA, 2028)
        }.build()

        assertEquals(listOf(2025, 2026, 2028), coverage.years(DocApi.HOLIDAY, Country.KOREA))
        assertEquals(listOf(ApiMeta.YearRange(2027, 2027), ApiMeta.YearRange(2029, 2030)), coverage.missing(DocApi.HOLIDAY, Country.KOREA, 2025..2030))
        assertEquals(listOf(ApiMeta.YearRange(2025, 2030)), coverage.missing(DocApi.LUNAR, Country.KOREA, 2025..2030))
    }
}
