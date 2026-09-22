package io.github.taetae98coding.calendar.data.source

/** 요청 한 건이 덮는 구간. 응답 단위를 그대로 따르며, 캐시 파일 하나의 단위이기도 하다. */
enum class Granularity {
    YEAR,
    MONTH,
}
