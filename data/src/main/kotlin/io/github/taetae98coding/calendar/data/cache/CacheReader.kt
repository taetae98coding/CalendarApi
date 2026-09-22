package io.github.taetae98coding.calendar.data.cache

import io.github.taetae98coding.calendar.core.Logger
import io.github.taetae98coding.calendar.data.source.SourceApi
import kotlinx.serialization.json.JsonElement

/**
 * 캐시 원본을 항목으로 해석한다. 저장소 구현이 읽는 쪽 입구다.
 *
 * 없거나 깨진 구간은 비어 있는 것으로 보고 로그만 남긴다. 다음 갱신이 다시 채운다.
 * 어떤 원천의 어떤 항목으로 읽을지는 호출자가 [parse] 로 정한다. 여기는 원천을 모른다.
 */
class CacheReader(
    private val store: CacheStore,
    private val logger: Logger,
) {
    suspend fun <T> items(api: SourceApi, period: CachePeriod, parse: (raw: JsonElement) -> List<T>): List<T> {
        return runCatching { store.read(api, period)?.let(parse).orEmpty() }
            .getOrElse { throwable ->
                logger.log("[Data] ${api.id} ${period.key} 해석 실패: ${throwable.message}")

                emptyList()
            }
    }
}
