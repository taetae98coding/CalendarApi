package io.github.taetae98coding.calendar.data

import io.github.taetae98coding.calendar.data.cache.CachePaths
import io.github.taetae98coding.calendar.data.cache.CachePeriod
import io.github.taetae98coding.calendar.data.cache.SourceApi
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class CachePathsTest {
    /** 캐시 경로는 응답 단위를 그대로 따른다. */
    @Test
    fun `월 단위 API 는 연도 폴더 아래 월 파일이다`() {
        assertEquals(
            "cache/kasi/getLunCalInfo/2026/01.json",
            CachePaths.file(SourceApi.KASI_LUN_CAL, CachePeriod(2026, 1)).invariantPath(),
        )
        assertEquals(
            "cache/kasi/getRestDeInfo/2050/12.json",
            CachePaths.file(SourceApi.KASI_REST_DE, CachePeriod(2050, 12)).invariantPath(),
        )
    }

    @Test
    fun `연 단위 API 는 월 경로가 없다`() {
        assertEquals(
            "cache/nager/publicHolidays-us/2026.json",
            CachePaths.file(SourceApi.NAGER_UNITED_STATES, CachePeriod(2026)).invariantPath(),
        )
    }

    @Test
    fun `갱신 기록은 API 당 하나다`() {
        assertEquals("cache/kasi/getLunCalInfo/meta.json", CachePaths.meta(SourceApi.KASI_LUN_CAL).invariantPath())
        assertEquals("cache/nager/publicHolidays-kr/meta.json", CachePaths.meta(SourceApi.NAGER_KOREA).invariantPath())
    }

    private fun File.invariantPath(): String = path.replace(File.separatorChar, '/')
}
