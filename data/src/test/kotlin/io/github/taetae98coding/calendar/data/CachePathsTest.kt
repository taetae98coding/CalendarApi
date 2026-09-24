package io.github.taetae98coding.calendar.data

import io.github.taetae98coding.calendar.core.file.invariantPath
import io.github.taetae98coding.calendar.data.cache.CachePaths
import io.github.taetae98coding.calendar.data.cache.CachePeriod
import io.github.taetae98coding.calendar.data.source.SourceApi
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** 캐시 경로가 바뀌면 받아 둔 자료를 전부 다시 받아야 한다. 고정해 둔다. */
class CachePathsTest {
    private val paths = CachePaths(File("cache"))

    @Test
    fun `월 단위 API 는 연도 폴더 아래 월 파일이다`() {
        assertEquals("cache/kasi/getLunCalInfo/2026/01.json", paths.file(SourceApi.KASI_LUN_CAL, CachePeriod(2026, 1)).invariantPath())
        assertEquals("cache/kasi/getRestDeInfo/2050/12.json", paths.file(SourceApi.KASI_REST_DE, CachePeriod(2050, 12)).invariantPath())
    }

    @Test
    fun `연 단위 API 는 월 경로가 없다`() {
        assertEquals("cache/nager/publicHolidays-us/2026.json", paths.file(SourceApi.NAGER_UNITED_STATES, CachePeriod(2026)).invariantPath())
        assertEquals("cache/nager/publicHolidays-kr/2026.json", paths.file(SourceApi.NAGER_KOREA, CachePeriod(2026)).invariantPath())
    }

    @Test
    fun `갱신 기록은 API 당 하나다`() {
        assertEquals("cache/kasi/getLunCalInfo/meta.json", paths.meta(SourceApi.KASI_LUN_CAL).invariantPath())
        assertEquals("cache/nager/publicHolidays-kr/meta.json", paths.meta(SourceApi.NAGER_KOREA).invariantPath())
    }
}
