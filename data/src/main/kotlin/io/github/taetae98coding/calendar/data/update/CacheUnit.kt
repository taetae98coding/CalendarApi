package io.github.taetae98coding.calendar.data.update

import io.github.taetae98coding.calendar.data.cache.CachePeriod
import io.github.taetae98coding.calendar.data.source.SourceApi
import kotlin.math.abs
import kotlin.time.Instant

/** 갱신 단위인 요청 한 건. [lastUpdatedAt] 이 null 이면 한 번도 받지 않았다. */
data class CacheUnit(
    val api: SourceApi,
    val period: CachePeriod,
    val lastUpdatedAt: Instant?,
) {
    val description: String
        get() = "${api.id} ${period.key}"
}

/**
 * 마지막 갱신이 오래된 순서.
 *
 * 예산이나 서버 문제로 중간에 멈춰도 다음 실행이 멈춘 지점부터 이어받아 전체가 골고루 갱신된다.
 * 한 번도 받지 않은 구간이 가장 먼저 온다.
 * 같은 시각이면 오늘과 가까운 연도를 먼저 채워 실제로 많이 쓰이는 구간이 먼저 완성되게 한다.
 */
fun List<CacheUnit>.oldestFirst(currentYear: Int): List<CacheUnit> {
    return sortedWith(
        compareBy<CacheUnit, Instant?>(nullsFirst()) { unit -> unit.lastUpdatedAt }
            .thenBy { unit -> abs(unit.period.year - currentYear) }
            .thenBy { unit -> unit.period.month ?: 0 },
    )
}
