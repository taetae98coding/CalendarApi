package io.github.taetae98coding.calendar.data.cache

import kotlin.math.abs
import kotlinx.datetime.YearMonth

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
}

/** 갱신 단위인 요청 한 건. [lastUpdatedAt] 이 null 이면 한 번도 받지 않았다. */
data class CacheUnit(
    val api: SourceApi,
    val period: CachePeriod,
    val lastUpdatedAt: String?,
)

/**
 * 마지막 갱신이 오래된 순서.
 *
 * 예산이나 서버 문제로 중간에 멈춰도 다음 실행이 멈춘 지점부터 이어받아 전체가 골고루 갱신된다.
 * [lastUpdatedAt] 은 ISO-8601 UTC 문자열이라 사전순 비교가 곧 시간순 비교이고,
 * 한 번도 받지 않은 구간은 빈 문자열로 취급되어 가장 먼저 온다.
 * 같은 시각이면 오늘과 가까운 연도를 먼저 채워 실제로 많이 쓰이는 구간이 먼저 완성되게 한다.
 */
fun List<CacheUnit>.oldestFirst(currentYear: Int): List<CacheUnit> {
    return sortedWith(
        compareBy(
            { unit -> unit.lastUpdatedAt ?: "" },
            { unit -> abs(unit.period.year - currentYear) },
            { unit -> unit.period.month ?: 0 },
        ),
    )
}
