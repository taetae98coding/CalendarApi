package io.github.taetae98coding.calendar.data.cache

import io.github.taetae98coding.calendar.data.source.SourceApi
import io.github.taetae98coding.calendar.datasource.file.JsonFiles
import kotlinx.serialization.json.JsonElement

/**
 * `cache/{provider}/{api}/` 를 읽고 쓴다.
 *
 * 응답 원본은 기계만 읽으므로 공백 없이, 갱신 기록은 diff 로 확인하므로 들여쓴다.
 */
class CacheStore(
    private val paths: CachePaths,
) {
    suspend fun meta(api: SourceApi): CacheMeta = JsonFiles.pretty.readOrNull(paths.meta(api)) ?: CacheMeta()

    suspend fun writeMeta(api: SourceApi, meta: CacheMeta) = JsonFiles.pretty.write(meta, paths.meta(api))

    /** 캐시가 없으면 null. 있는데 깨졌으면 예외가 난다. */
    suspend fun read(api: SourceApi, period: CachePeriod): JsonElement? = JsonFiles.compact.readOrNull(paths.file(api, period))

    suspend fun write(api: SourceApi, period: CachePeriod, raw: JsonElement) = JsonFiles.compact.write(raw, paths.file(api, period))
}
