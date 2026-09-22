package io.github.taetae98coding.calendar.openapi.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 공공데이터포털 응답. 정상이면 [response], 인증키·트래픽 오류면 [serviceError] 가 채워진다.
 * 두 경우 모두 `application/json` 이라 하나의 타입으로 받아 [bodyOrThrow] 에서 구분한다.
 */
@Serializable
data class OpenApiResult<T>(
    @SerialName("response")
    val response: OpenApiResponse<T>? = null,
    @SerialName("OpenAPI_ServiceResponse")
    val serviceError: OpenApiServiceError? = null,
) {
    fun bodyOrThrow(description: String): T {
        serviceError?.header?.let { header ->
            throw OpenApiException(header.code, "$description 실패. code=${header.code}, message=${header.message}, auth=${header.authMessage}")
        }

        val response = requireNotNull(response) { "$description 실패. 응답에 response 가 없습니다." }
        if (response.header.code != SUCCESS_CODE) {
            throw OpenApiException(response.header.code, "$description 실패. code=${response.header.code}, message=${response.header.message}")
        }

        return response.body
    }

    companion object {
        const val SUCCESS_CODE = "00"
    }
}
