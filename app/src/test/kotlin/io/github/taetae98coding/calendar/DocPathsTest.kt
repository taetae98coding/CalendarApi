package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.domain.holiday.Country
import io.github.taetae98coding.calendar.publish.DocApi
import io.github.taetae98coding.calendar.publish.DocPaths
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.YearMonth

/** 배포 경로는 곧 공개 URL 이다. 바뀌면 소비처가 깨지므로 고정해 둔다. */
class DocPathsTest {
    @Test
    fun `배포는 API - 국가 - 연도 - 월 네 단계다`() {
        assertEquals("docs/holiday/kr/2026.json", DocPaths.year(DocApi.HOLIDAY, Country.KOREA, 2026).invariantPath())
        assertEquals("docs/holiday/kr/2026/01.json", DocPaths.month(DocApi.HOLIDAY, Country.KOREA, YearMonth(2026, 1)).invariantPath())
        assertEquals("docs/calendar/us/2050/12.json", DocPaths.month(DocApi.CALENDAR, Country.UNITED_STATES, YearMonth(2050, 12)).invariantPath())
        assertEquals("docs/lunar/kr/1998.json", DocPaths.year(DocApi.LUNAR, Country.KOREA, 1998).invariantPath())
    }

    private fun File.invariantPath(): String = path.replace(File.separatorChar, '/')
}
