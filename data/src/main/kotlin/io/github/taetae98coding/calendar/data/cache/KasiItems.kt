package io.github.taetae98coding.calendar.data.cache

import io.github.taetae98coding.calendar.datasource.openapi.OpenApiClient
import io.github.taetae98coding.calendar.datasource.openapi.entity.OpenApiResult
import io.github.taetae98coding.calendar.datasource.openapi.kasi.KasiBody
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * KASI 응답에 항목이 들어 있는가.
 *
 * 특일 정보와 음양력 정보가 같은 봉투를 쓰므로 두 도메인이 같은 판정을 공유한다.
 * 봉투가 깨졌으면 예외가 나고, 호출하는 쪽이 "항목 없음"으로 본다.
 */
internal fun hasKasiItems(api: SourceApi, raw: JsonElement): Boolean {
    return OpenApiClient.json.decodeFromJsonElement<OpenApiResult<KasiBody>>(raw)
        .bodyOrThrow(api.id)
        .count > 0
}
