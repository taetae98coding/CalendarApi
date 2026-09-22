package io.github.taetae98coding.calendar.data

import io.github.taetae98coding.calendar.data.cache.CacheMeta
import io.github.taetae98coding.calendar.data.cache.CachePaths
import io.github.taetae98coding.calendar.data.cache.CachePeriod
import io.github.taetae98coding.calendar.data.cache.CacheStore
import io.github.taetae98coding.calendar.data.cache.CacheUpdater
import io.github.taetae98coding.calendar.data.source.FetchService
import io.github.taetae98coding.calendar.data.source.SourceApi
import io.github.taetae98coding.calendar.data.source.SourceFetcher
import io.github.taetae98coding.calendar.datasource.kasi.OpenApiException
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

class CacheUpdaterTest {
    private val root = tempDirectory()
    private val paths = CachePaths(root)
    private val store = CacheStore(paths)
    private val logger = RecordingLogger()
    private val clock = FixedClock(Instant.parse("2026-09-22T00:00:00Z"))
    private val years = 2025..2026

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    /** 요청 내역을 기록하고 정해진 답을 돌려주는 가짜 원천. */
    private class FakeFetcher(
        private val answer: (SourceApi, CachePeriod) -> Result<JsonElement?> = { _, period -> Result.success(JsonPrimitive(period.key)) },
    ) : SourceFetcher {
        val requests = mutableListOf<Pair<SourceApi, CachePeriod>>()
        val count = AtomicInteger()

        override suspend fun fetch(api: SourceApi, period: CachePeriod): Result<JsonElement?> {
            synchronized(requests) { requests += api to period }
            count.incrementAndGet()

            return answer(api, period)
        }

        override suspend fun unavailable(): Set<FetchService> = emptySet()
    }

    private fun updater(fetcher: SourceFetcher) = CacheUpdater(store, fetcher, logger, clock, TimeZone.UTC, years)

    @Test
    fun `예산만큼만 요청하고 성공한 구간만 기록한다`() = runTest {
        val fetcher = FakeFetcher()

        val reports = updater(fetcher).update(fetchBudget = 5, skip = setOf(FetchService.KASI_SPCDE, FetchService.KASI_LUNAR))

        val report = reports.getValue(FetchService.NAGER)
        assertEquals(4, report.units) // 국가 2 × 연도 2
        assertEquals(4, report.succeeded)
        assertEquals(4, report.requests)
        assertEquals(1, report.remainingBudget)
        assertFalse(FetchService.KASI_SPCDE in reports)

        val meta = store.meta(SourceApi.NAGER_KOREA)
        assertEquals(setOf("2025", "2026"), meta.lastUpdatedAt.keys)
        assertEquals(clock.now, meta.lastUpdatedAt.getValue("2026"))
        assertTrue(paths.file(SourceApi.NAGER_KOREA, CachePeriod(2026)).exists())
    }

    @Test
    fun `예산이 모자라면 오래된 구간부터 받는다`() = runTest {
        val old = Instant.parse("2026-01-01T00:00:00Z")
        store.writeMeta(SourceApi.NAGER_KOREA, CacheMeta(mapOf("2025" to clock.now, "2026" to old)))
        store.writeMeta(SourceApi.NAGER_UNITED_STATES, CacheMeta(mapOf("2025" to clock.now, "2026" to clock.now)))
        val fetcher = FakeFetcher()

        updater(fetcher).update(fetchBudget = 1, skip = setOf(FetchService.KASI_SPCDE, FetchService.KASI_LUNAR))

        assertEquals(listOf(SourceApi.NAGER_KOREA to CachePeriod(2026)), fetcher.requests)
        // 받지 않은 구간은 이전 기록을 그대로 유지한다.
        assertEquals(clock.now, store.meta(SourceApi.NAGER_UNITED_STATES).lastUpdatedAt.getValue("2025"))
    }

    @Test
    fun `남길 항목이 없는 응답은 성공이지만 캐시를 덮어쓰지 않는다`() = runTest {
        store.write(SourceApi.NAGER_KOREA, CachePeriod(2026), JsonPrimitive("kept"))
        val fetcher = FakeFetcher { _, _ -> Result.success(null) }

        val report = updater(fetcher).update(fetchBudget = 10, skip = setOf(FetchService.KASI_SPCDE, FetchService.KASI_LUNAR)).getValue(FetchService.NAGER)

        assertEquals(4, report.succeeded)
        assertEquals(JsonPrimitive("kept"), store.read(SourceApi.NAGER_KOREA, CachePeriod(2026)))
        assertNull(store.read(SourceApi.NAGER_KOREA, CachePeriod(2025)))
        assertEquals(setOf("2025", "2026"), store.meta(SourceApi.NAGER_KOREA).lastUpdatedAt.keys)
    }

    @Test
    fun `일일 트래픽을 다 쓰면 서비스를 멈추고 예산을 돌려준다`() = runTest {
        val fetcher = FakeFetcher { _, _ -> Result.failure(OpenApiException(OpenApiException.QUOTA_EXCEEDED_CODE, "quota")) }

        val report = updater(fetcher).update(fetchBudget = 100, skip = setOf(FetchService.KASI_SPCDE, FetchService.NAGER)).getValue(FetchService.KASI_LUNAR)

        assertEquals(0, report.succeeded)
        assertTrue(fetcher.count.get() < 24, "멈추지 않고 ${fetcher.count.get()}건을 보냈습니다.")
        assertEquals(100 - report.requests, report.remainingBudget)
        assertEquals(1, logger.messages.count { message -> "일일 트래픽" in message })
        assertTrue(store.meta(SourceApi.KASI_LUN_CAL).lastUpdatedAt.isEmpty())
    }

    @Test
    fun `속도 초과는 예산을 돌려주고 한 번만 알린다`() = runTest {
        val fetcher = FakeFetcher { _, _ -> Result.failure(OpenApiException(OpenApiException.RATE_LIMIT_CODE, "rate")) }

        val report = updater(fetcher).update(fetchBudget = 100, skip = setOf(FetchService.KASI_SPCDE, FetchService.NAGER)).getValue(FetchService.KASI_LUNAR)

        assertEquals(24, fetcher.count.get())
        assertEquals(0, report.requests)
        assertEquals(1, logger.messages.count { message -> "속도 초과" in message })
    }

    @Test
    fun `그 외 실패는 구간마다 알리고 예산은 소비된다`() = runTest {
        val fetcher = FakeFetcher { api, period ->
            if (period.year == 2025) Result.failure(IllegalStateException("boom")) else Result.success(JsonPrimitive(api.id))
        }

        val report = updater(fetcher).update(fetchBudget = 100, skip = setOf(FetchService.KASI_SPCDE, FetchService.KASI_LUNAR)).getValue(FetchService.NAGER)

        assertEquals(2, report.succeeded)
        assertEquals(4, report.requests)
        assertEquals(2, logger.messages.count { message -> "조회 실패" in message && "boom" in message })
        assertEquals(setOf("2026"), store.meta(SourceApi.NAGER_KOREA).lastUpdatedAt.keys)
    }

    @Test
    fun `범위 밖으로 밀려난 기록은 지운다`() = runTest {
        store.writeMeta(SourceApi.NAGER_KOREA, CacheMeta(mapOf("1997" to clock.now, "2025" to clock.now)))

        updater(FakeFetcher()).update(fetchBudget = 0)

        assertEquals(setOf("2025"), store.meta(SourceApi.NAGER_KOREA).lastUpdatedAt.keys)
    }

    @Test
    fun `건너뛴 서비스는 손대지 않는다`() = runTest {
        val fetcher = FakeFetcher()

        val reports = updater(fetcher).update(fetchBudget = 100, skip = FetchService.entries.toSet())

        assertTrue(reports.isEmpty())
        assertTrue(fetcher.requests.isEmpty())
        assertFalse(File(root, "kasi").exists())
    }
}
