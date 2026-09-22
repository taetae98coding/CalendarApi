package io.github.taetae98coding.calendar.data.cache

import io.github.taetae98coding.calendar.datasource.file.FileDataSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * API 하나의 갱신 기록. 키는 [CachePeriod.key] 다.
 *
 * 구간마다 파일을 따로 두면 수천 개가 되고 실행할 때마다 그만큼 diff 가 생기므로 API 당 하나로 모은다.
 */
@Serializable
data class CacheMeta(
    @SerialName("lastUpdatedAt")
    val lastUpdatedAt: Map<String, String> = emptyMap(),
)

/** `cache/{provider}/{api}/` 를 읽고 쓴다. */
data object CacheStore {
    suspend fun meta(api: SourceApi): CacheMeta {
        return FileDataSource.readOrNull<CacheMeta>(CachePaths.meta(api)) ?: CacheMeta()
    }

    suspend fun writeMeta(api: SourceApi, meta: CacheMeta) {
        FileDataSource.write(meta, CachePaths.meta(api))
    }

    suspend fun read(api: SourceApi, period: CachePeriod): JsonElement? {
        return FileDataSource.readOrNull(CachePaths.file(api, period))
    }

    suspend fun write(api: SourceApi, period: CachePeriod, raw: JsonElement) {
        FileDataSource.writeCache(raw, CachePaths.file(api, period))
    }
}
