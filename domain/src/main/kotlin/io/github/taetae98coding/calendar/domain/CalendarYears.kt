package io.github.taetae98coding.calendar.domain

/**
 * 모든 API 가 제공하는 고정 범위.
 *
 * 원천 API 가 실제로 제공하는 범위는 서비스마다 다르고 언제 바뀔지 모른다.
 * 제공 여부와 무관하게 이 범위로 고정해 두고, 없는 구간은 비어 있는 채로 둔다.
 *
 * 수집(`:data`)과 배포(`:app`)가 같은 범위를 봐야 하므로 도메인에 둔다.
 */
data object CalendarYears {
    const val START_YEAR = 1998

    /** @see START_YEAR */
    const val END_INCLUSIVE_YEAR = 2050

    val years: IntRange = START_YEAR..END_INCLUSIVE_YEAR
}
