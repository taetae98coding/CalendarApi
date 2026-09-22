package io.github.taetae98coding.calendar.datasource

import io.github.taetae98coding.calendar.datasource.openapi.RateLimiter
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest

class RateLimiterTest {
    /** runTest 는 delay 를 실제로 기다리지 않고 가상 시계만 앞당기므로 testScheduler 로 확인한다. */
    @Test
    fun `초당 허용량을 넘지 않게 간격을 벌린다`() = runTest {
        val limiter = RateLimiter(permitsPerSecond = 20)

        // 20건을 한꺼번에 요청해도 20 req/s 면 마지막 건은 약 1초 뒤에 통과해야 한다.
        coroutineScope {
            List(20) { async { limiter.acquire() } }.awaitAll()
        }

        val elapsed = testScheduler.currentTime

        assertTrue(elapsed >= 900, "20건이 ${elapsed}ms 만에 통과했습니다. 간격이 벌어지지 않았습니다.")
    }

    @Test
    fun `여유가 있으면 기다리지 않는다`() = runTest {
        val limiter = RateLimiter(permitsPerSecond = 1000)

        repeat(5) { limiter.acquire() }

        val elapsed = testScheduler.currentTime

        assertTrue(elapsed < 100, "${elapsed}ms 를 기다렸습니다.")
    }
}
