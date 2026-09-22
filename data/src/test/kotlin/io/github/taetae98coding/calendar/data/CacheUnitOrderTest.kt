package io.github.taetae98coding.calendar.data

import io.github.taetae98coding.calendar.data.cache.CachePeriod
import io.github.taetae98coding.calendar.data.source.Granularity
import io.github.taetae98coding.calendar.data.source.SourceApi
import io.github.taetae98coding.calendar.data.update.CacheUnit
import io.github.taetae98coding.calendar.data.update.oldestFirst
import io.github.taetae98coding.calendar.domain.CalendarYears
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class CacheUnitOrderTest {
    private fun unit(year: Int, month: Int?, lastUpdatedAt: String?): CacheUnit {
        val api = if (month == null) SourceApi.NAGER_KOREA else SourceApi.KASI_REST_DE

        return CacheUnit(api, CachePeriod(year, month), lastUpdatedAt?.let(Instant::parse))
    }

    @Test
    fun `한 번도 받지 않은 구간이 가장 먼저 온다`() {
        val units = listOf(
            unit(2020, null, "2026-09-22T00:00:00Z"),
            unit(2021, null, null),
            unit(2022, null, "2026-09-20T00:00:00Z"),
        )

        assertEquals(listOf(2021, 2022, 2020), units.oldestFirst(2026).map { unit -> unit.period.year })
    }

    @Test
    fun `갱신 시각이 같으면 올해와 가까운 연도를 먼저 채운다`() {
        val units = listOf(2050, 2026, 1998, 2028).map { year -> unit(year, null, null) }

        assertEquals(listOf(2026, 2028, 2050, 1998), units.oldestFirst(2026).map { unit -> unit.period.year })
    }

    /** 갱신 단위가 요청 한 건이라, 한 달만 실패하면 그 달만 다음 차례로 밀린다. */
    @Test
    fun `같은 해에서도 실패한 달만 먼저 잡힌다`() {
        val units = (1..12).map { month -> unit(2026, month, if (month == 7) null else "2026-09-22T00:00:00Z") }

        assertEquals(7, units.oldestFirst(2026).first().period.month)
    }

    @Test
    fun `이어받기가 남은 구간부터 시작한다`() {
        val updated = (1998..2023).map { year -> unit(year, null, "2026-09-22T00:00:00Z") }
        val remaining = (2024..2050).map { year -> unit(year, null, null) }

        val next = (updated + remaining).oldestFirst(2026).take(remaining.size)

        assertEquals(remaining.map { unit -> unit.period.year }.toSet(), next.map { unit -> unit.period.year }.toSet())
    }

    @Test
    fun `연 단위 API 는 구간에 월이 없다`() {
        assertEquals(Granularity.YEAR, SourceApi.NAGER_KOREA.granularity)
        assertEquals("2026", CachePeriod(2026).key)
        assertEquals("2026/01", CachePeriod(2026, 1).key)
        assertEquals(53, SourceApi.NAGER_KOREA.periods(CalendarYears.years).size)
        assertEquals(53 * 12, SourceApi.KASI_REST_DE.periods(CalendarYears.years).size)
    }
}
