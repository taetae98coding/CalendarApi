package io.github.taetae98coding.calendar.data.source

/** 캐시 경로의 첫 단계. 원천 제공처 하나. */
enum class CacheProvider(
    val id: String,
) {
    KASI("kasi"),
    NAGER("nager"),
}
