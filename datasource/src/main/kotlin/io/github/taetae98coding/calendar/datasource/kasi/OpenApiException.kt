package io.github.taetae98coding.calendar.datasource.kasi

import io.github.taetae98coding.calendar.datasource.SourceException

/** 공공데이터포털이 내려준 오류. [code] 를 원천 무관한 [kind] 로 옮긴다. */
class OpenApiException(
    val code: String,
    message: String,
) : SourceException(kindOf(code), message) {
    companion object {
        const val NOT_REGISTERED_CODE = "30"
        const val RATE_LIMIT_CODE = "23"
        const val QUOTA_EXCEEDED_CODE = "22"
        const val NO_DATA_CODE = "03"

        private fun kindOf(code: String): Kind {
            return when (code) {
                NOT_REGISTERED_CODE -> Kind.NOT_REGISTERED
                RATE_LIMIT_CODE -> Kind.RATE_LIMITED
                QUOTA_EXCEEDED_CODE -> Kind.QUOTA_EXCEEDED
                NO_DATA_CODE -> Kind.NO_DATA
                else -> Kind.OTHER
            }
        }
    }
}
