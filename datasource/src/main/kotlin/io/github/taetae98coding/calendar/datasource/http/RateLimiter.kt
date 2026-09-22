package io.github.taetae98coding.calendar.datasource.http

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 초당 요청 수를 고르게 제한한다.
 *
 * data.go.kr 은 짧은 시간에 요청이 몰리면 HTTP 429 와 함께 code=23 을 돌려준다.
 * 동시 요청 수(Semaphore)만 막아서는 응답이 빠를 때 초당 수십 건이 나가므로 속도 자체를 제한해야 한다.
 *
 * 시계를 주입받으므로 테스트에서는 가상 시계로 간격을 확인할 수 있다.
 */
class RateLimiter(
    permitsPerSecond: Int,
    timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val interval: Duration = 1.seconds / permitsPerSecond.coerceAtLeast(1)
    private val origin = timeSource.markNow()
    private val mutex = Mutex()
    private var nextAvailable: Duration = Duration.ZERO

    suspend fun acquire() {
        val wait = mutex.withLock {
            val now = origin.elapsedNow()
            val scheduled = maxOf(now, nextAvailable)
            nextAvailable = scheduled + interval

            scheduled - now
        }

        if (wait.isPositive()) delay(wait)
    }
}
