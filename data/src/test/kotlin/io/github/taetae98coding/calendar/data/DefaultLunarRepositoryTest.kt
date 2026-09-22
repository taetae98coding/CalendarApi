package io.github.taetae98coding.calendar.data

import io.github.taetae98coding.calendar.data.cache.CachePaths
import io.github.taetae98coding.calendar.data.cache.CachePeriod
import io.github.taetae98coding.calendar.data.cache.CacheStore
import io.github.taetae98coding.calendar.data.lunar.DefaultLunarRepository
import io.github.taetae98coding.calendar.data.source.SourceApi
import io.github.taetae98coding.calendar.domain.lunar.LunarDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class DefaultLunarRepositoryTest {
    private val root = tempDirectory()
    private val store = CacheStore(CachePaths(root))
    private val repository = DefaultLunarRepository(store, RecordingLogger())

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    private suspend fun cache(period: CachePeriod, body: String) = store.write(SourceApi.KASI_LUN_CAL, period, Json.parseToJsonElement(body))

    @Test
    fun `응답의 실제 양력 날짜로 연도를 거르고 중복을 없앤다`() = runTest {
        cache(CachePeriod(2025, 1), Fixtures.lunar(Fixtures.lunarItem("2025-01-01", 2024, 12, 2), Fixtures.lunarItem("2025-01-29", 2025, 1, 1)))
        // 12월 응답에 이듬해 1월 1일이 섞여 오고, 1월 1일이 두 번 들어 있는 경우.
        cache(CachePeriod(2025, 12), Fixtures.lunar(Fixtures.lunarItem("2025-12-31", 2025, 11, 12), Fixtures.lunarItem("2026-01-01", 2025, 11, 13), Fixtures.lunarItem("2025-01-01", 2024, 12, 2)))

        val lunar = repository.get(2025)

        assertEquals(listOf(LocalDate(2025, 1, 1), LocalDate(2025, 1, 29), LocalDate(2025, 12, 31)), lunar.map(LunarDate::solar))
        assertEquals(LunarDate(LocalDate(2025, 1, 29), 2025, 1, 1, isLeapMonth = false), lunar[1])
    }

    @Test
    fun `캐시가 없으면 비어 있다`() = runTest {
        assertTrue(repository.get(2030).isEmpty())
    }
}
