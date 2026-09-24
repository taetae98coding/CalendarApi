package io.github.taetae98coding.calendar.data

import io.github.taetae98coding.calendar.data.cache.CacheMeta
import io.github.taetae98coding.calendar.data.cache.CachePaths
import io.github.taetae98coding.calendar.data.cache.CachePeriod
import io.github.taetae98coding.calendar.data.cache.CacheStore
import io.github.taetae98coding.calendar.data.source.FetchService
import io.github.taetae98coding.calendar.data.source.SourceApi
import io.github.taetae98coding.calendar.data.source.SourceFetcher
import io.github.taetae98coding.calendar.data.update.CacheUpdater
import io.github.taetae98coding.calendar.data.update.StopReason
import io.github.taetae98coding.calendar.datasource.SourceException
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

    private fun updater(fetcher: SourceFetcher, consecutiveFailureLimit: Int = 100) =
        CacheUpdater(store, fetcher, logger, clock, TimeZone.UTC, years, consecutiveFailureLimit)

    @Test
    fun `멈출 이유가 없으면 전 구간을 돌고 성공한 구간만 기록한다`() = runTest {
        val fetcher = FakeFetcher()

        val reports = updater(fetcher).update(skip = setOf(FetchService.KASI_SPCDE, FetchService.KASI_LUNAR))

        val report = reports.getValue(FetchService.NAGER)
        assertEquals(4, report.units) // 국가 2 × 연도 2
        assertEquals(4, report.succeeded)
        assertEquals(4, report.requests)
        assertNull(report.stoppedBy)
        assertFalse(FetchService.KASI_SPCDE in reports)

        val meta = store.meta(SourceApi.NAGER_KOREA)
        assertEquals(setOf("2025", "2026"), meta.lastUpdatedAt.keys)
        assertEquals(clock.now, meta.lastUpdatedAt.getValue("2026"))
        assertTrue(paths.file(SourceApi.NAGER_KOREA, CachePeriod(2026)).exists())
    }

    @Test
    fun `멈춘 뒤에도 갱신에 성공한 구간만 시각을 덮는다`() = runTest {
        val old = Instant.parse("2026-01-01T00:00:00Z")
        // 한 번도 받지 않은 구간이 먼저 오므로, 순서를 정하려면 두 API 모두 기록이 있어야 한다.
        store.writeMeta(SourceApi.NAGER_KOREA, CacheMeta(mapOf("2025" to old, "2026" to old)))
        store.writeMeta(SourceApi.NAGER_UNITED_STATES, CacheMeta(mapOf("2025" to old, "2026" to old)))
        // 올해와 가까운 2026 만 성공하고 2025 는 한도에 걸린다.
        val fetcher = FakeFetcher { _, period ->
            if (period.year == 2026) {
                Result.success(JsonPrimitive(period.key))
            } else {
                Result.failure(SourceException(SourceException.Kind.QUOTA_EXCEEDED, "quota"))
            }
        }

        updater(fetcher).update(skip = setOf(FetchService.KASI_SPCDE, FetchService.KASI_LUNAR))

        val meta = store.meta(SourceApi.NAGER_KOREA).lastUpdatedAt
        assertEquals(clock.now, meta.getValue("2026"))
        // 받지 못한 구간은 이전 기록을 그대로 유지해 다음 실행이 먼저 잡는다.
        assertEquals(old, meta.getValue("2025"))
    }

    @Test
    fun `남길 항목이 없는 응답은 성공이지만 캐시를 덮어쓰지 않는다`() = runTest {
        store.write(SourceApi.NAGER_KOREA, CachePeriod(2026), JsonPrimitive("kept"))
        val fetcher = FakeFetcher { _, _ -> Result.success(null) }

        val report = updater(fetcher).update(skip = setOf(FetchService.KASI_SPCDE, FetchService.KASI_LUNAR)).getValue(FetchService.NAGER)

        assertEquals(4, report.succeeded)
        assertEquals(JsonPrimitive("kept"), store.read(SourceApi.NAGER_KOREA, CachePeriod(2026)))
        assertNull(store.read(SourceApi.NAGER_KOREA, CachePeriod(2025)))
        assertEquals(setOf("2025", "2026"), store.meta(SourceApi.NAGER_KOREA).lastUpdatedAt.keys)
    }

    @Test
    fun `일일 트래픽을 다 쓰면 서비스를 멈춘다`() = runTest {
        val fetcher = FakeFetcher { _, _ -> Result.failure(SourceException(SourceException.Kind.QUOTA_EXCEEDED, "quota")) }

        val report = updater(fetcher).update(skip = setOf(FetchService.KASI_SPCDE, FetchService.NAGER)).getValue(FetchService.KASI_LUNAR)

        assertEquals(0, report.succeeded)
        assertEquals(StopReason.QUOTA_EXCEEDED, report.stoppedBy)
        assertTrue(fetcher.count.get() < 24, "멈추지 않고 ${fetcher.count.get()}건을 보냈습니다.")
        assertEquals(1, logger.messages.count { message -> "일일 트래픽" in message })
        assertTrue(store.meta(SourceApi.KASI_LUN_CAL).lastUpdatedAt.isEmpty())
    }

    @Test
    fun `연속 실패가 한계에 닿으면 남은 구간을 멈춘다`() = runTest {
        val fetcher = FakeFetcher { _, _ -> Result.failure(IllegalStateException("boom")) }

        val report = updater(fetcher, consecutiveFailureLimit = 3)
            .update(skip = setOf(FetchService.KASI_SPCDE, FetchService.NAGER))
            .getValue(FetchService.KASI_LUNAR)

        assertEquals(StopReason.REPEATED_FAILURE, report.stoppedBy)
        assertTrue(fetcher.count.get() < 24, "멈추지 않고 ${fetcher.count.get()}건을 보냈습니다.")
        assertEquals(1, logger.messages.count { message -> "연속 3건" in message })
    }

    /** 원천이 과거 자료를 고치는 일이 실제로 있다. 받아 둔 구간이라고 건너뛰면 그 변경을 영영 못 본다. */
    @Test
    fun `이미 받아 둔 구간도 매번 다시 받는다`() = runTest {
        years.forEach { year -> store.write(SourceApi.NAGER_KOREA, CachePeriod(year), JsonPrimitive("stale")) }
        store.writeMeta(SourceApi.NAGER_KOREA, CacheMeta(years.associate { year -> CachePeriod(year).key to clock.now }))
        val fetcher = FakeFetcher()

        val report = updater(fetcher).update(skip = setOf(FetchService.KASI_SPCDE, FetchService.KASI_LUNAR)).getValue(FetchService.NAGER)

        assertEquals(4, report.units)
        assertEquals(4, report.requests)
        assertEquals(JsonPrimitive("2026"), store.read(SourceApi.NAGER_KOREA, CachePeriod(2026)))
    }

    @Test
    fun `속도 초과는 한 번만 알린다`() = runTest {
        val fetcher = FakeFetcher { _, _ -> Result.failure(SourceException(SourceException.Kind.RATE_LIMITED, "rate")) }

        updater(fetcher).update(skip = setOf(FetchService.KASI_SPCDE, FetchService.NAGER)).getValue(FetchService.KASI_LUNAR)

        assertEquals(24, fetcher.count.get())
        assertEquals(1, logger.messages.count { message -> "속도 초과" in message })
    }

    @Test
    fun `그 외 실패는 구간마다 알린다`() = runTest {
        val fetcher = FakeFetcher { api, period ->
            if (period.year == 2025) Result.failure(IllegalStateException("boom")) else Result.success(JsonPrimitive(api.id))
        }

        val report = updater(fetcher).update(skip = setOf(FetchService.KASI_SPCDE, FetchService.KASI_LUNAR)).getValue(FetchService.NAGER)

        assertEquals(2, report.succeeded)
        assertEquals(4, report.requests)
        assertEquals(2, logger.messages.count { message -> "조회 실패" in message && "boom" in message })
        assertEquals(setOf("2026"), store.meta(SourceApi.NAGER_KOREA).lastUpdatedAt.keys)
    }

    @Test
    fun `범위 밖으로 밀려난 기록은 지운다`() = runTest {
        store.writeMeta(SourceApi.NAGER_KOREA, CacheMeta(mapOf("1997" to clock.now, "2025" to clock.now)))

        updater(FakeFetcher()).update()

        assertEquals(setOf("2025", "2026"), store.meta(SourceApi.NAGER_KOREA).lastUpdatedAt.keys)
    }

    @Test
    fun `건너뛴 서비스는 손대지 않는다`() = runTest {
        val fetcher = FakeFetcher()

        val reports = updater(fetcher).update(skip = FetchService.entries.toSet())

        assertTrue(reports.isEmpty())
        assertTrue(fetcher.requests.isEmpty())
        assertFalse(File(root, "kasi").exists())
    }
}
