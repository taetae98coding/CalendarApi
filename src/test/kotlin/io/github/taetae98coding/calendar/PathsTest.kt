package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.cache.CachePeriod
import io.github.taetae98coding.calendar.cache.SourceApi
import io.github.taetae98coding.calendar.docs.DocApi
import io.github.taetae98coding.calendar.holiday.Country
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.YearMonth

class PathsTest {
    /** 캐시 경로는 응답 단위를 그대로 따른다. */
    @Test
    fun `월 단위 API 는 연도 폴더 아래 월 파일이다`() {
        assertEquals(
            "cache/kasi/getLunCalInfo/2026/01.json",
            Paths.cacheFile(SourceApi.KASI_LUN_CAL, CachePeriod(2026, 1)).invariantPath(),
        )
        assertEquals(
            "cache/kasi/getRestDeInfo/2050/12.json",
            Paths.cacheFile(SourceApi.KASI_REST_DE, CachePeriod(2050, 12)).invariantPath(),
        )
    }

    @Test
    fun `연 단위 API 는 월 경로가 없다`() {
        assertEquals(
            "cache/nager/publicHolidays-us/2026.json",
            Paths.cacheFile(SourceApi.NAGER_UNITED_STATES, CachePeriod(2026)).invariantPath(),
        )
    }

    @Test
    fun `갱신 기록은 API 당 하나다`() {
        assertEquals("cache/kasi/getLunCalInfo/meta.json", Paths.cacheMeta(SourceApi.KASI_LUN_CAL).invariantPath())
        assertEquals("cache/nager/publicHolidays-kr/meta.json", Paths.cacheMeta(SourceApi.NAGER_KOREA).invariantPath())
    }

    @Test
    fun `배포는 API - 국가 - 연도 - 월 네 단계다`() {
        assertEquals("docs/holiday/kr/2026.json", Paths.docYear(DocApi.HOLIDAY, Country.KOREA, 2026).invariantPath())
        assertEquals("docs/holiday/kr/2026/01.json", Paths.docMonth(DocApi.HOLIDAY, Country.KOREA, YearMonth(2026, 1)).invariantPath())
        assertEquals("docs/calendar/us/2050/12.json", Paths.docMonth(DocApi.CALENDAR, Country.UNITED_STATES, YearMonth(2050, 12)).invariantPath())
        assertEquals("docs/lunar/kr/1998.json", Paths.docYear(DocApi.LUNAR, Country.KOREA, 1998).invariantPath())
    }

    private fun File.invariantPath(): String = path.replace(File.separatorChar, '/')
}
