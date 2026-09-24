package io.github.taetae98coding.calendar.data.update

/** 서비스 하나의 갱신 결과. 로그와 테스트가 같은 숫자를 본다. */
data class UpdateReport(
    val units: Int,
    val succeeded: Int,
    val requests: Int,
    /** 전 구간을 돌기 전에 멈췄다면 그 이유. 끝까지 돌았으면 null. */
    val stoppedBy: StopReason?,
)
