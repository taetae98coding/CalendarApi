package io.github.taetae98coding.calendar.datasource.openapi.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 공공데이터포털이 인증키·트래픽 오류에서 정상 응답 대신 내려주는 봉투. */
@Serializable
data class OpenApiServiceError(
    @SerialName("cmmMsgHeader")
    val header: Header,
) {
    @Serializable
    data class Header(
        @SerialName("returnReasonCode")
        val code: String = "",
        @SerialName("errMsg")
        val message: String = "",
        @SerialName("returnAuthMsg")
        val authMessage: String = "",
    )
}
