package io.github.taetae98coding.calendar.openapi.entity

/** 공공데이터포털이 내려준 오류. [code] 로 원인을 구분할 수 있다. */
class OpenApiException(
    val code: String,
    override val message: String,
) : IllegalStateException(message) {
    val isNotRegistered: Boolean
        get() = code == NOT_REGISTERED_CODE

    companion object {
        /** 해당 API 에 활용신청이 되어 있지 않을 때의 코드. */
        const val NOT_REGISTERED_CODE = "30"
    }
}
