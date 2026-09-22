package io.github.taetae98coding.calendar.cache

import io.github.taetae98coding.calendar.Config
import io.github.taetae98coding.calendar.openapi.OpenApiClient
import io.github.taetae98coding.calendar.openapi.entity.OpenApiException
import io.github.taetae98coding.calendar.openapi.entity.OpenApiResult
import io.github.taetae98coding.calendar.openapi.kasi.KasiBody
import io.github.taetae98coding.calendar.openapi.kasi.KasiDataSource
import io.github.taetae98coding.calendar.openapi.kasi.KasiService
import io.github.taetae98coding.calendar.openapi.nager.NagerDataSource
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * 캐시를 갱신한다. 범위는 [Config.years] 로 고정이고 조건부 생략이 없다. 매 실행마다 전 구간을 다시 받는다.
 *
 * 다만 한 번에 다 받을 수는 없으므로 서비스마다 [Config.fetchBudget] 만큼만 요청하고,
 * **마지막 갱신이 오래된 구간부터** 처리한다.
 * 갱신 단위는 요청 한 건과 같아서, 월 단위 API 는 한 달만 실패하면 그 달만 다음 차례로 밀린다.
 */
data object CacheUpdater {
    /**
     * 동시에 처리할 구간 수.
     *
     * 실제 요청 수는 [OpenApiClient.maxConcurrency] 가 막으므로 여기서는 "오래된 것부터" 순서를 지키는 역할만 한다.
     * 수천 개를 한꺼번에 띄우면 예산을 가져가는 순서가 정렬과 무관해진다.
     */
    private const val UNIT_PARALLELISM = 16

    suspend fun update(config: Config, registrations: Map<KasiService, Boolean>) {
        coroutineScope {
            FetchService.entries
                .filter { service -> service.kasiService?.let(registrations::getValue) != false }
                .map { service -> async { updateService(service, config) } }
                .awaitAll()
        }
    }

    private suspend fun updateService(service: FetchService, config: Config) {
        val apis = SourceApi.entries.filter { api -> api.service == service }
        val metas = apis.associateWith { api -> CacheStore.meta(api) }
        val units = oldestFirst(apis, metas)

        val state = ServiceState(config.fetchBudget)
        val semaphore = Semaphore(UNIT_PARALLELISM)

        val succeeded = coroutineScope {
            units.map { unit -> async { unit.takeIf { semaphore.withPermit { updateUnit(unit, state) } } } }
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

            api.periods(Config.years).map { period -> CacheUnit(api, period, lastUpdatedAt[period.key]) }
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
                    val keys = api.periods(Config.years).map(CachePeriod::key).toSet()
                    val meta = CacheMeta((metas.getValue(api).lastUpdatedAt + updated).filterKeys(keys::contains))

                    CacheStore.writeMeta(api, meta)
                }
            }
        }
    }

    private suspend fun updateUnit(unit: CacheUnit, state: ServiceState): Boolean {
        val description = "${unit.api.id} ${unit.period.key}"
        if (!state.takeBudget()) return false

        val result = when (unit.api.service) {
            FetchService.KASI_LUNAR -> KasiDataSource.getLunar(unit.period.yearMonth)
            FetchService.KASI_SPCDE -> KasiDataSource.getSpcde(unit.api.id, unit.period.yearMonth)
            FetchService.NAGER -> {
                val country = requireNotNull(unit.api.country) { "${unit.api} 에 국가가 없습니다." }

                NagerDataSource.getHoliday(unit.period.year, country.nagerCode)
            }
        }

        return result.fold(
            onSuccess = { raw ->
                // 응답이 정상이면 성공이다. 항목이 있을 때만 캐시를 덮어써 일시적인 빈 응답으로 기존 자료를 잃지 않는다.
                if (raw != null && hasItems(unit.api, raw, description)) CacheStore.write(unit.api, unit.period, raw)

                true
            },
            onFailure = { throwable ->
                state.onFailure(description, throwable)
                false
            },
        )
    }

    private fun hasItems(api: SourceApi, raw: JsonElement, description: String): Boolean {
        return runCatching {
            when (api.provider) {
                CacheProvider.KASI -> {
                    OpenApiClient.json.decodeFromJsonElement<OpenApiResult<KasiBody>>(raw)
                        .bodyOrThrow(description)
                        .count > 0
                }

                CacheProvider.NAGER -> NagerDataSource.parseHolidays(raw).isNotEmpty()
            }
        }
            .getOrDefault(false)
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
