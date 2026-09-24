package io.github.taetae98coding.calendar.core

import java.util.concurrent.CopyOnWriteArrayList

/** 로그를 모아 두고 확인한다. 여러 코루틴이 동시에 남겨도 안전하다. */
class RecordingLogger : Logger {
    private val recorded = CopyOnWriteArrayList<String>()

    val messages: List<String>
        get() = recorded

    override fun log(message: String) {
        recorded += message
    }

    fun count(fragment: String): Int = recorded.count { message -> fragment in message }
}
