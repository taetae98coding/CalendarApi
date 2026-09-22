package io.github.taetae98coding.calendar.holiday

/** API 가 제공하는 국가. [code] 가 그대로 URL 경로가 된다. */
enum class Country(
    val code: String,
    val nagerCode: String,
    val displayName: String,
) {
    KOREA("kr", "KR", "대한민국"),
    UNITED_STATES("us", "US", "미국"),
}
