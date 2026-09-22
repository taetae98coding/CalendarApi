package io.github.taetae98coding.calendar.publish

import io.github.taetae98coding.calendar.domain.Country
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.YearMonth

/** 배포 경로는 곧 공개 URL 이다. 바뀌면 소비처가 깨지므로 고정해 둔다. */
class DocPathsTest {
    private val paths = DocPaths(File("docs"))

    @Test
    fun `배포는 API - 국가 - 연도 - 월 네 단계다`() {
        assertEquals("docs/holiday/kr/2026.json", paths.year(DocApi.HOLIDAY, Country.KOREA, 2026).invariantPath())
        assertEquals("docs/holiday/kr/2026/01.json", paths.month(DocApi.HOLIDAY, Country.KOREA, YearMonth(2026, 1)).invariantPath())
        assertEquals("docs/calendar/us/2050/12.json", paths.month(DocApi.CALENDAR, Country.UNITED_STATES, YearMonth(2050, 12)).invariantPath())
        assertEquals("docs/lunar/kr/1998.json", paths.year(DocApi.LUNAR, Country.KOREA, 1998).invariantPath())
    }

    @Test
    fun `루트 파일은 세 개다`() {
        assertEquals("docs/meta.json", paths.meta.invariantPath())
        assertEquals("docs/index.html", paths.index.invariantPath())
        assertEquals("docs/.nojekyll", paths.noJekyll.invariantPath())
    }

    private fun File.invariantPath(): String = path.replace(File.separatorChar, '/')
}
