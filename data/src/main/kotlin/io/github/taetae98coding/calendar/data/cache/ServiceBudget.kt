package io.github.taetae98coding.calendar.data.cache

import io.github.taetae98coding.calendar.data.Logger
import io.github.taetae98coding.calendar.datasource.kasi.OpenApiException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 서비스 하나의 예산과 중단 여부.
 *
 * 속도 초과는 호출이 성립하지 않은 것이므로 예산을 돌려주고,
 * 일일 트래픽을 다 쓰면 남은 요청을 모두 멈춰 다음 실행이 이어받게 한다.
 * 수천 건이 한꺼번에 실패할 수 있어 같은 사유는 한 번만 출력한다.
 */
class ServiceBudget(
    private val initial: Int,
    private val logger: Logger,
) {
    private val budget = AtomicInteger(initial)
    private val stopped = AtomicBoolean(false)
    private val rateLimitLogged = AtomicBoolean(false)

    val remaining: Int
        get() = budget.get()

    val used: Int
        get() = initial - budget.get()

    /** 요청 하나를 보낼 수 있으면 예산을 하나 차감하고 참. */
    fun take(): Boolean {
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
                    logger.log("[Cache] 일일 트래픽을 모두 사용했습니다. 남은 구간은 다음 실행에서 이어서 갱신합니다.")
                }
            }

            exception?.isRateLimited == true -> {
                budget.incrementAndGet()
                if (rateLimitLogged.compareAndSet(false, true)) {
                    logger.log("[Cache] 요청 속도 초과로 일부 요청을 건너뜁니다. MAX_REQUESTS_PER_SECOND 를 낮춰 보세요.")
                }
            }

            else -> logger.log("[Cache] $description 조회 실패: ${throwable.message}")
        }
    }
}
