package io.github.taetae98coding.calendar.datasource.openapi.entity

/** 공공데이터포털이 내려준 오류. [code] 로 원인을 구분할 수 있다. */
class OpenApiException(
    val code: String,
    override val message: String,
) : IllegalStateException(message) {
    /** 해당 API 에 활용신청이 되어 있지 않다. */
    val isNotRegistered: Boolean
        get() = code == NOT_REGISTERED_CODE

    /** 초당 요청 수를 넘겼다. 잠시 뒤 다시 하면 된다. */
    val isRateLimited: Boolean
        get() = code == RATE_LIMIT_CODE

    /** 일일 트래픽을 다 썼다. 오늘은 더 호출해도 소용없다. */
    val isQuotaExceeded: Boolean
        get() = code == QUOTA_EXCEEDED_CODE

    /** 요청은 정상이고 해당 구간에 자료가 없을 뿐이다. 404 와 같이 취급한다. */
    val isNoData: Boolean
        get() = code == NO_DATA_CODE

    companion object {
        const val NOT_REGISTERED_CODE = "30"
        const val RATE_LIMIT_CODE = "23"
        const val QUOTA_EXCEEDED_CODE = "22"
        const val NO_DATA_CODE = "03"
    }
}
