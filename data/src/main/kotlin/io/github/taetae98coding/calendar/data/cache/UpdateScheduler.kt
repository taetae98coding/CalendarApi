package io.github.taetae98coding.calendar.data.cache

import io.github.taetae98coding.calendar.datasource.openapi.entity.OpenApiException
import io.github.taetae98coding.calendar.datasource.openapi.kasi.KasiService
import io.github.taetae98coding.calendar.domain.CalendarYears
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
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
 * 캐시 갱신의 예산·순서·동시성을 관리한다. 무엇을 받는지는 [SourceUpdater] 가 안다.
 *
 * 범위는 [CalendarYears.years] 로 고정이고 조건부 생략이 없다. 매 실행마다 전 구간을 다시 받는다.
 * 다만 한 번에 다 받을 수는 없으므로 서비스마다 예산만큼만 요청하고 **마지막 갱신이 오래된 구간부터** 처리한다.
 * 갱신 단위는 요청 한 건과 같아서, 월 단위 API 는 한 달만 실패하면 그 달만 다음 차례로 밀린다.
 *
 * 예산과 순서를 [SourceUpdater] 가 아니라 여기서 서비스 단위로 묶는 이유가 있다.
 * 공공데이터포털은 인증키가 하나여도 서비스마다 한도를 따로 세므로, 한 서비스를 두 도메인이 나눠 쓰게 되어도
 * 한도는 여전히 하나다. 도메인마다 예산을 따로 들면 그 순간 한도를 두 배로 쓰게 된다.
 */
data object UpdateScheduler {
    /**
     * 동시에 처리할 구간 수.
     *
     * 실제 요청 수는 원천별 동시성 제한이 막으므로 여기서는 "오래된 것부터" 순서를 지키는 역할만 한다.
     * 수천 개를 한꺼번에 띄우면 예산을 가져가는 순서가 정렬과 무관해진다.
     */
    private const val UNIT_PARALLELISM = 16

    suspend fun update(updaters: List<SourceUpdater>, fetchBudget: Int, registrations: Map<KasiService, Boolean>) {
        val byApi = updaters.flatMap { updater -> updater.apis.map { api -> api to updater } }.toMap()

        coroutineScope {
            FetchService.entries
                .filter { service -> service.kasiService?.let(registrations::getValue) != false }
                .map { service -> async { updateService(service, byApi, fetchBudget) } }
                .awaitAll()
        }
    }

    private suspend fun updateService(service: FetchService, byApi: Map<SourceApi, SourceUpdater>, fetchBudget: Int) {
        val apis = SourceApi.entries.filter { api -> api.service == service && api in byApi }
        if (apis.isEmpty()) return

        val metas = apis.associateWith { api -> CacheStore.meta(api) }
        val units = oldestFirst(apis, metas)

        val state = ServiceState(fetchBudget)
        val semaphore = Semaphore(UNIT_PARALLELISM)

        val succeeded = coroutineScope {
            units.map { unit -> async { unit.takeIf { semaphore.withPermit { updateUnit(unit, byApi.getValue(unit.api), state) } } } }
                .awaitAll()
        }
            .filterNotNull()

        writeMeta(apis, metas, succeeded)

        println("[Cache] $service : ${units.size}개 구간 중 ${succeeded.size}개 갱신, 요청 ${state.used}건, 잔여 예산 ${state.remaining}")
    }

    /**
     * 한 번도 받지 않은 구간이 먼저 오고, 그 안에서는 오늘과 가까운 연도를 먼저 채운다.
     * [Semaphore] 가 대체로 FIFO 라 이 순서가 예산을 가져가는 순서로 이어진다.
     */
    private fun oldestFirst(apis: List<SourceApi>, metas: Map<SourceApi, CacheMeta>): List<CacheUnit> {
        val today = Clock.System.todayIn(TimeZone.of("Asia/Seoul"))

        return apis.flatMap { api ->
            val lastUpdatedAt = metas.getValue(api).lastUpdatedAt

            api.periods(CalendarYears.years).map { period -> CacheUnit(api, period, lastUpdatedAt[period.key]) }
        }
            .oldestFirst(today.year)
    }

    /**
     * 갱신에 성공한 구간만 이번 시각으로 덮고 나머지는 이전 값을 유지한다.
     * 범위 밖으로 밀려난 키는 이 과정에서 함께 사라진다.
     */
    private suspend fun writeMeta(apis: List<SourceApi>, metas: Map<SourceApi, CacheMeta>, succeeded: List<CacheUnit>) {
        val now = Clock.System.now().toString()
        val succeededKeys = succeeded.groupBy(CacheUnit::api) { unit -> unit.period.key }

        coroutineScope {
            apis.forEach { api ->
                launch {
                    val updated = succeededKeys[api].orEmpty().associateWith { now }
                    val keys = api.periods(CalendarYears.years).map(CachePeriod::key).toSet()
                    val meta = CacheMeta((metas.getValue(api).lastUpdatedAt + updated).filterKeys(keys::contains))

                    CacheStore.writeMeta(api, meta)
                }
            }
        }
    }

    private suspend fun updateUnit(unit: CacheUnit, updater: SourceUpdater, state: ServiceState): Boolean {
        val description = "${unit.api.id} ${unit.period.key}"
        if (!state.takeBudget()) return false

        return updater.fetch(unit).fold(
            onSuccess = { raw ->
                // 응답이 정상이면 성공이다. 항목이 있을 때만 캐시를 덮어써 일시적인 빈 응답으로 기존 자료를 잃지 않는다.
                if (raw != null && runCatching { updater.hasItems(unit.api, raw) }.getOrDefault(false)) {
                    CacheStore.write(unit.api, unit.period, raw)
                }

                true
            },
            onFailure = { throwable ->
                state.onFailure(description, throwable)
                false
            },
        )
    }

    /**
     * 서비스 하나의 예산과 중단 여부.
     *
     * 속도 초과는 호출이 성립하지 않은 것이므로 예산을 돌려주고,
     * 일일 트래픽을 다 쓰면 남은 요청을 모두 멈춰 다음 실행이 이어받게 한다.
     * 수천 건이 한꺼번에 실패할 수 있어 같은 사유는 한 번만 출력한다.
     */
    private class ServiceState(
        private val initialBudget: Int,
    ) {
        private val budget = AtomicInteger(initialBudget)
        private val stopped = AtomicBoolean(false)
        private val rateLimitLogged = AtomicBoolean(false)

        val remaining: Int
            get() = budget.get()

        val used: Int
            get() = initialBudget - budget.get()

        fun takeBudget(): Boolean {
            if (stopped.get()) return false

            while (true) {
                val current = budget.get()
                if (current <= 0) return false
                if (budget.compareAndSet(current, current - 1)) return true
            }
        }

        fun onFailure(description: String, throwable: Throwable) {
            val exception = throwable as? OpenApiException

            when {
                exception?.isQuotaExceeded == true -> {
                    budget.incrementAndGet()
                    if (stopped.compareAndSet(false, true)) {
                        println("[Cache] 일일 트래픽을 모두 사용했습니다. 남은 구간은 다음 실행에서 이어서 갱신합니다.")
                    }
                }

                exception?.isRateLimited == true -> {
                    budget.incrementAndGet()
                    if (rateLimitLogged.compareAndSet(false, true)) {
                        println("[Cache] 요청 속도 초과로 일부 요청을 건너뜁니다. MAX_REQUESTS_PER_SECOND 를 낮춰 보세요.")
                    }
                }

                else -> println("[Cache] $description 조회 실패: ${throwable.message}")
            }
        }
    }
}
