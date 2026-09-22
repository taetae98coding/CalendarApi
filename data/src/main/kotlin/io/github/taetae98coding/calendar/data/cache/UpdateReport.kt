package io.github.taetae98coding.calendar.data.cache

/** 서비스 하나의 갱신 결과. 로그와 테스트가 같은 숫자를 본다. */
data class UpdateReport(
    val units: Int,
    val succeeded: Int,
    val requests: Int,
    val remainingBudget: Int,
)
