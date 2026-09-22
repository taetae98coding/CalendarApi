package io.github.taetae98coding.calendar.datasource

import io.github.taetae98coding.calendar.datasource.http.RateLimiter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class RateLimiterTest {
    /** runTest 는 delay 를 실제로 기다리지 않고 가상 시계만 앞당긴다. 같은 시계를 제한기에도 넘겨 간격을 확인한다. */
    @Test
    fun `초당 허용량을 넘지 않게 간격을 벌린다`() = runTest {
        val limiter = RateLimiter(permitsPerSecond = 20, timeSource = testScheduler.timeSource)

        // 20건을 한꺼번에 요청하면 20 req/s 에서 마지막 건은 950ms 뒤에 통과한다.
        coroutineScope {
            List(20) { async { limiter.acquire() } }.awaitAll()
        }

        assertEquals(950, testScheduler.currentTime)
    }

    @Test
    fun `여유가 있으면 기다리지 않는다`() = runTest {
        val limiter = RateLimiter(permitsPerSecond = 1000, timeSource = testScheduler.timeSource)

        limiter.acquire()
        testScheduler.advanceTimeBy(10)
        limiter.acquire()

        assertTrue(testScheduler.currentTime <= 10, "${testScheduler.currentTime}ms 를 기다렸습니다.")
    }
}
