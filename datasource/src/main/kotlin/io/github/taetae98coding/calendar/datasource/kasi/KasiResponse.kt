package io.github.taetae98coding.calendar.datasource.kasi

import io.github.taetae98coding.calendar.datasource.http.HttpClients
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * KASI 응답 원본을 해석한다.
 *
 * 방금 받은 응답이든 캐시에 저장해 둔 원본이든 같은 방식으로 읽는다.
 * 봉투가 오류를 담고 있으면 [OpenApiException] 이 난다.
 */
object KasiResponse {
    val json: Json = HttpClients.json

    fun body(raw: JsonElement, description: String): KasiBody {
        return json.decodeFromJsonElement<OpenApiResult<KasiBody>>(raw).bodyOrThrow(description)
    }

    inline fun <reified T> items(raw: JsonElement, description: String): List<T> {
        return body(raw, description).items(json)
    }
}
