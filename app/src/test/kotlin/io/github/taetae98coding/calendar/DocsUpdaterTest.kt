package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.domain.Country
import io.github.taetae98coding.calendar.domain.holiday.Holiday
import io.github.taetae98coding.calendar.domain.holiday.HolidayRepository
import io.github.taetae98coding.calendar.domain.lunar.LunarDate
import io.github.taetae98coding.calendar.domain.lunar.LunarRepository
import io.github.taetae98coding.calendar.publish.CalendarResponse
import io.github.taetae98coding.calendar.publish.DocApi
import io.github.taetae98coding.calendar.publish.DocPaths
import io.github.taetae98coding.calendar.publish.DocsUpdater
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.serialization.json.Json

class DocsUpdaterTest {
    private val root = createTempDirectory("calendar-api-docs").toFile()
    private val paths = DocPaths(root)
    private val json = Json { ignoreUnknownKeys = true }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    private val holidayRepository = HolidayRepository { country, year ->
        when {
            country == Country.KOREA && year == 2025 -> listOf(
                Holiday("신정", true, LocalDate(2025, 1, 1), LocalDate(2025, 1, 1)),
                Holiday("설날", true, LocalDate(2025, 1, 28), LocalDate(2025, 1, 30)),
            )
            else -> emptyList()
        }
    }

    private val lunarRepository = LunarRepository { year ->
        if (year == 2025) listOf(LunarDate(LocalDate(2025, 1, 1), 2024, 12, 2, false), LunarDate(LocalDate(2025, 2, 1), 2025, 1, 4, false)) else emptyList()
    }

    @Test
    fun `범위 전체에 연 파일과 월 파일을 국가별로 만든다`() = runTest {
        DocsUpdater(paths, holidayRepository, lunarRepository, years = 2025..2026).update()

        val files = root.walk().filter(File::isFile).count()

        // API 3 × 국가 2 × 연도 2 × (연 1 + 월 12)
        assertEquals(3 * 2 * 2 * 13, files)
        assertEquals("[]", paths.year(DocApi.HOLIDAY, Country.UNITED_STATES, 2026).readText().trim())
    }

    @Test
    fun `월 파일은 그 달에 걸친 공휴일과 그 달의 음력만 담는다`() = runTest {
        DocsUpdater(paths, holidayRepository, lunarRepository, years = 2025..2025).update()

        val january = json.decodeFromString<CalendarResponse>(paths.month(DocApi.CALENDAR, Country.KOREA, YearMonth(2025, 1)).readText())
        val february = json.decodeFromString<CalendarResponse>(paths.month(DocApi.CALENDAR, Country.KOREA, YearMonth(2025, 2)).readText())
        val year = json.decodeFromString<CalendarResponse>(paths.year(DocApi.CALENDAR, Country.KOREA, 2025).readText())

        assertEquals("2025-01", january.period)
        assertEquals(listOf("신정", "설날"), january.holidays.map(Holiday::name))
        assertEquals(listOf(LocalDate(2025, 1, 1)), january.lunar.map(LunarDate::solar))
        assertTrue(february.holidays.isEmpty())
        assertEquals(listOf(LocalDate(2025, 2, 1)), february.lunar.map(LunarDate::solar))
        assertEquals("2025", year.period)
        assertEquals("kr", year.country)
        assertEquals(2, year.lunar.size)
    }

    @Test
    fun `음력은 국가와 무관하게 같은 내용이 국가별 경로에 쓰인다`() = runTest {
        DocsUpdater(paths, holidayRepository, lunarRepository, years = 2025..2025).update()

        assertEquals(paths.year(DocApi.LUNAR, Country.KOREA, 2025).readText(), paths.year(DocApi.LUNAR, Country.UNITED_STATES, 2025).readText())
    }

    @Test
    fun `생성 현황은 파일이 아니라 내용이 있는 연도를 센다`() = runTest {
        val coverage = DocsUpdater(paths, holidayRepository, lunarRepository, years = 2025..2026).update()

        assertEquals(listOf(2025), coverage.years(DocApi.HOLIDAY, Country.KOREA))
        assertEquals(emptyList(), coverage.years(DocApi.HOLIDAY, Country.UNITED_STATES))
        assertEquals(listOf(2025), coverage.years(DocApi.LUNAR, Country.UNITED_STATES))
        assertEquals(listOf(2025), coverage.years(DocApi.CALENDAR, Country.UNITED_STATES))
    }
}
