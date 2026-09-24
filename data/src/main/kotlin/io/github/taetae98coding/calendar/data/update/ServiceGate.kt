package io.github.taetae98coding.calendar.data.update

import io.github.taetae98coding.calendar.core.Logger
import io.github.taetae98coding.calendar.datasource.SourceException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 서비스 하나의 중단 여부.
 *
 * 요청 수를 미리 정하지 않는다. 원천이 일일 트래픽을 다 썼다고 알려 오거나 실패가 연달아 쌓이면
 * 남은 요청을 모두 멈춰 다음 실행이 이어받게 한다.
 * 속도 초과와 일일 트래픽 초과는 호출이 성립하지 않은 것이므로 요청 수에 넣지 않는다.
 * 속도 초과는 우리 쪽 설정 문제라 연속 실패로도 세지 않는다.
 * 수천 건이 한꺼번에 실패할 수 있어 같은 사유는 한 번만 출력한다.
 *
 * 실패 사유는 [SourceException.kind] 로만 본다. 어느 원천이 어떤 코드로 실패했는지는 여기서 모른다.
 */
class ServiceGate(
    private val logger: Logger,
    private val consecutiveFailureLimit: Int = DEFAULT_CONSECUTIVE_FAILURE_LIMIT,
) {
    private val requestedCount = AtomicInteger(0)
    private val consecutiveFailures = AtomicInteger(0)
    private val stopReason = AtomicReference<StopReason?>(null)
    private val rateLimitLogged = AtomicBoolean(false)

    /** 실제로 성립한 요청 수. */
    val requested: Int
        get() = requestedCount.get()

    /** 멈췄다면 그 이유. */
    val stoppedBy: StopReason?
        get() = stopReason.get()

    /** 요청 하나를 보내도 되면 세고 참. */
    fun take(): Boolean {
        if (stopReason.get() != null) return false

        requestedCount.incrementAndGet()
        return true
    }

    fun onSuccess() {
        consecutiveFailures.set(0)
    }

    fun onFailure(description: String, throwable: Throwable) {
        val exception = throwable as? SourceException

        when {
            exception?.isQuotaExceeded == true -> {
                requestedCount.decrementAndGet()
                if (stop(StopReason.QUOTA_EXCEEDED)) {
                    logger.log("[Cache] 일일 트래픽을 모두 사용했습니다. 남은 구간은 다음 실행에서 이어서 갱신합니다.")
                }
            }

            exception?.isRateLimited == true -> {
                requestedCount.decrementAndGet()
                if (rateLimitLogged.compareAndSet(false, true)) {
                    logger.log("[Cache] 요청 속도 초과로 일부 요청을 건너뜁니다. MAX_REQUESTS_PER_SECOND 를 낮춰 보세요.")
                }
            }

            else -> {
                logger.log("[Cache] $description 조회 실패: ${throwable.message}")
                if (consecutiveFailures.incrementAndGet() >= consecutiveFailureLimit && stop(StopReason.REPEATED_FAILURE)) {
                    logger.log("[Cache] 연속 ${consecutiveFailureLimit}건 실패해 남은 구간을 멈춥니다. 다음 실행에서 이어서 갱신합니다.")
                }
            }
        }
    }

    /** 처음 멈춘 호출만 참. */
    private fun stop(reason: StopReason): Boolean = stopReason.compareAndSet(null, reason)

    companion object {
        /**
         * 이만큼 연달아 실패하면 원천이 통째로 응답하지 않는 것으로 본다.
         *
         * 구간을 16개씩 동시에 처리하므로 장애 한 번에 그만큼은 한꺼번에 실패한다. 그보다 넉넉히 잡는다.
         */
        const val DEFAULT_CONSECUTIVE_FAILURE_LIMIT = 50
    }
}
