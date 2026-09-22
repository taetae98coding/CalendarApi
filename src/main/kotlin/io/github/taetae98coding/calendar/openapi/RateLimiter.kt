package io.github.taetae98coding.calendar.openapi

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 초당 요청 수를 고르게 제한한다.
 *
 * data.go.kr 은 짧은 시간에 요청이 몰리면 HTTP 429 와 함께 code=23 을 돌려준다.
 * 동시 요청 수(Semaphore)만 막아서는 응답이 빠를 때 초당 수십 건이 나가므로 속도 자체를 제한해야 한다.
 */
class RateLimiter(
    permitsPerSecond: Int,
) {
    private val intervalNanos = 1_000_000_000L / permitsPerSecond.coerceAtLeast(1)
    private val mutex = Mutex()
    private var nextAvailableNanos = 0L

    suspend fun acquire() {
        val waitNanos = mutex.withLock {
            val now = System.nanoTime()
            val scheduled = maxOf(now, nextAvailableNanos)
            nextAvailableNanos = scheduled + intervalNanos

            scheduled - now
        }

        if (waitNanos > 0) delay(waitNanos / 1_000_000)
    }
}
