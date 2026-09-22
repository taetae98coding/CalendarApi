package io.github.taetae98coding.calendar.data.update

import io.github.taetae98coding.calendar.core.Logger
import io.github.taetae98coding.calendar.data.cache.CacheMeta
import io.github.taetae98coding.calendar.data.cache.CachePeriod
import io.github.taetae98coding.calendar.data.cache.CacheStore
import io.github.taetae98coding.calendar.data.source.FetchService
import io.github.taetae98coding.calendar.data.source.SourceApi
import io.github.taetae98coding.calendar.data.source.SourceFetcher
import io.github.taetae98coding.calendar.domain.CalendarYears
import kotlin.time.Clock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * 캐시 갱신의 예산·순서·동시성을 관리한다. 무엇을 어떻게 받는지는 [SourceFetcher] 가 안다.
 *
 * 범위는 [years] 로 고정이고 조건부 생략이 없다. 매 실행마다 전 구간을 다시 받는다.
 * 다만 한 번에 다 받을 수는 없으므로 서비스마다 예산만큼만 요청하고 **마지막 갱신이 오래된 구간부터** 처리한다.
 * 갱신 단위는 요청 한 건과 같아서, 월 단위 API 는 한 달만 실패하면 그 달만 다음 차례로 밀린다.
 *
 * 예산과 순서를 도메인이 아니라 서비스 단위로 묶는 이유가 있다.
 * 공공데이터포털은 인증키가 하나여도 서비스마다 한도를 따로 세므로, 한 서비스를 두 도메인이 나눠 쓰게 되어도
 * 한도는 여전히 하나다. 도메인마다 예산을 따로 들면 그 순간 한도를 두 배로 쓰게 된다.
 */
class CacheUpdater(
    private val store: CacheStore,
    private val fetcher: SourceFetcher,
    private val logger: Logger,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.of("Asia/Seoul"),
    private val years: IntRange = CalendarYears.years,
) {
    /** 서비스마다 [fetchBudget] 건까지 요청한다. [skip] 에 든 서비스는 건드리지 않는다. */
    suspend fun update(fetchBudget: Int, skip: Set<FetchService> = emptySet()): Map<FetchService, UpdateReport> {
        return coroutineScope {
            FetchService.entries
                .filterNot(skip::contains)
                .map { service -> async { service to updateService(service, fetchBudget) } }
                .awaitAll()
                .toMap()
        }
    }

    private suspend fun updateService(service: FetchService, fetchBudget: Int): UpdateReport {
        val apis = SourceApi.entries.filter { api -> api.service == service }
        val metas = apis.associateWith { api -> store.meta(api) }
        val units = units(apis, metas)

        val budget = ServiceBudget(fetchBudget, logger)
        val semaphore = Semaphore(UNIT_PARALLELISM)

        val succeeded = coroutineScope {
            units.map { unit -> async { unit.takeIf { semaphore.withPermit { updateUnit(unit, budget) } } } }
                .awaitAll()
        }
            .filterNotNull()

        writeMeta(apis, metas, succeeded)

        val report = UpdateReport(units = units.size, succeeded = succeeded.size, requests = budget.used, remainingBudget = budget.remaining)
        logger.log("[Cache] $service : ${report.units}개 구간 중 ${report.succeeded}개 갱신, 요청 ${report.requests}건, 잔여 예산 ${report.remainingBudget}")

        return report
    }

    /**
     * 한 번도 받지 않은 구간이 먼저 오고, 그 안에서는 오늘과 가까운 연도를 먼저 채운다.
     * [Semaphore] 가 대체로 FIFO 라 이 순서가 예산을 가져가는 순서로 이어진다.
     */
    private fun units(apis: List<SourceApi>, metas: Map<SourceApi, CacheMeta>): List<CacheUnit> {
        val today = clock.todayIn(timeZone)

        return apis.flatMap { api ->
            val lastUpdatedAt = metas.getValue(api).lastUpdatedAt

            api.periods(years).map { period -> CacheUnit(api, period, lastUpdatedAt[period.key]) }
        }
            .oldestFirst(today.year)
    }

    private suspend fun updateUnit(unit: CacheUnit, budget: ServiceBudget): Boolean {
        if (!budget.take()) return false

        return fetcher.fetch(unit.api, unit.period).fold(
            onSuccess = { raw ->
                // 응답이 정상이면 성공이다. 남길 원본이 있을 때만 덮어써 일시적인 빈 응답으로 기존 자료를 잃지 않는다.
                if (raw != null) store.write(unit.api, unit.period, raw)

                true
            },
            onFailure = { throwable ->
                budget.onFailure(unit.description, throwable)

                false
            },
        )
    }

    /**
     * 갱신에 성공한 구간만 이번 시각으로 덮고 나머지는 이전 값을 유지한다.
     * 범위 밖으로 밀려난 키는 이 과정에서 함께 사라진다.
     */
    private suspend fun writeMeta(apis: List<SourceApi>, metas: Map<SourceApi, CacheMeta>, succeeded: List<CacheUnit>) {
        val now = clock.now()
        val succeededKeys = succeeded.groupBy(CacheUnit::api) { unit -> unit.period.key }

        coroutineScope {
            apis.forEach { api ->
                launch {
                    val updated = succeededKeys[api].orEmpty().associateWith { now }
                    val keys = api.periods(years).map(CachePeriod::key).toSet()
                    val meta = CacheMeta((metas.getValue(api).lastUpdatedAt + updated).filterKeys(keys::contains))

                    store.writeMeta(api, meta)
                }
            }
        }
    }

    companion object {
        /**
         * 동시에 처리할 구간 수.
         *
         * 실제 요청 수는 원천별 동시성 제한이 막으므로 여기서는 "오래된 것부터" 순서를 지키는 역할만 한다.
         * 수천 개를 한꺼번에 띄우면 예산을 가져가는 순서가 정렬과 무관해진다.
         */
        private const val UNIT_PARALLELISM = 16
    }
}
