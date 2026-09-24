package io.github.taetae98coding.calendar.data.update

/** 서비스 하나가 전 구간을 돌기 전에 멈춘 이유. */
enum class StopReason {
    /** 원천이 일일 트래픽을 다 썼다고 알려 왔다. */
    QUOTA_EXCEEDED,

    /** 실패가 연달아 쌓였다. 원천이 통째로 응답하지 않는 상황으로 본다. */
    REPEATED_FAILURE,
}
