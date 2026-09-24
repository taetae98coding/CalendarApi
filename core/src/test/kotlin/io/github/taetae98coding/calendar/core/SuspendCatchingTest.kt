package io.github.taetae98coding.calendar.core

import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SuspendCatchingTest {
    @Test
    fun `보통 실패는 값으로 돌려준다`() {
        val result = runSuspendCatching { error("boom") }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(1, runSuspendCatching { 1 }.getOrThrow())
    }

    @Test
    fun `취소는 삼키지 않고 그대로 낸다`() {
        assertFailsWith<CancellationException> {
            runSuspendCatching { throw CancellationException("stop") }
        }
    }
}
