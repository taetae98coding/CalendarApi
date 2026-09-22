package io.github.taetae98coding.calendar.domain

/**
 * API 가 제공하는 국가. [code] 가 그대로 URL 경로가 된다.
 *
 * 원천마다 국가를 어떻게 부르는지는 여기 없다. 그 대응은 원천을 아는 `:data` 가 가진다.
 */
enum class Country(
    val code: String,
    val displayName: String,
) {
    KOREA("kr", "대한민국"),
    UNITED_STATES("us", "미국"),
}
