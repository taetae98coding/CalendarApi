package io.github.taetae98coding.calendar.data.cache

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API 하나의 갱신 기록. 키는 [CachePeriod.key] 다.
 *
 * 구간마다 파일을 따로 두면 수천 개가 되고 실행할 때마다 그만큼 diff 가 생기므로 API 당 하나로 모은다.
 */
@Serializable
data class CacheMeta(
    @SerialName("lastUpdatedAt")
    val lastUpdatedAt: Map<String, Instant> = emptyMap(),
)
