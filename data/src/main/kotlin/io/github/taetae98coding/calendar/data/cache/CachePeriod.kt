package io.github.taetae98coding.calendar.data.cache

import kotlinx.datetime.YearMonth
import kotlinx.datetime.number

/**
 * 요청 한 건이 덮는 구간. 캐시 파일 하나와 `meta.json` 항목 하나에 대응한다.
 *
 * [key] 가 그대로 캐시 파일의 상대 경로(`2026/01` · `2026`)이자 `meta.json` 의 키가 된다.
 */
data class CachePeriod(
    val year: Int,
    val month: Int? = null,
) {
    val key: String
        get() = if (month == null) "$year" else "$year/${month.toString().padStart(2, '0')}"

    val yearMonth: YearMonth
        get() = YearMonth(year, requireNotNull(month) { "$key 는 연 단위 구간이라 월이 없습니다." })

    companion object {
        fun of(yearMonth: YearMonth): CachePeriod = CachePeriod(yearMonth.year, yearMonth.month.number)
    }
}
