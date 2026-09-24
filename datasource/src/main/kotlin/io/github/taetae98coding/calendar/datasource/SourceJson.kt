package io.github.taetae98coding.calendar.datasource

import kotlinx.serialization.json.Json

/**
 * 원천 응답을 읽는 Json 설정.
 *
 * 응답을 받을 때(HTTP 클라이언트)와 캐시에 남긴 원본을 다시 해석할 때(KasiResponse · NagerResponse)가
 * 같은 설정을 써야 같은 결과가 나온다. 원천 응답은 필드가 자주 늘고 숫자가 문자열로 오기도 해서 느슨하게 읽는다.
 */
object SourceJson {
    val lenient: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
}
