package io.github.taetae98coding.calendar.core

import kotlin.time.Clock
import kotlin.time.Instant

/** 시각이 고정된 시계. 산출물에 찍히는 값을 예측할 수 있다. */
class FixedClock(
    var now: Instant,
) : Clock {
    override fun now(): Instant = now
}
