package io.github.taetae98coding.calendar.datasource.http

import kotlin.time.TimeSource
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * 외부 API 한 곳으로 나가는 요청의 동시 수와 속도를 함께 제한한다.
 *
 * 같은 호스트를 여러 클라이언트가 나눠 쓰면 하나의 [Throttle] 을 공유해야 한도가 한 번만 적용된다.
 */
class Throttle(
    maxConcurrency: Int,
    requestsPerSecond: Int,
    timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val semaphore = Semaphore(maxConcurrency.coerceAtLeast(1))
    private val rateLimiter = RateLimiter(requestsPerSecond, timeSource)

    suspend fun <T> withPermit(block: suspend () -> T): T {
        return semaphore.withPermit {
            rateLimiter.acquire()
            block()
        }
    }
}
