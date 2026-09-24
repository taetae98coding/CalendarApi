package io.github.taetae98coding.calendar.datasource

/**
 * 원천 호출이 실패한 이유.
 *
 * 갱신 엔진은 어느 원천이 어떤 코드로 실패했는지 모른다. [kind] 만 보고 요청으로 셀지, 서비스를 멈출지 정한다.
 * 원천마다 오류 표기가 다르므로 각 원천의 예외가 이 타입을 상속해 [kind] 로 옮겨 준다.
 */
open class SourceException(
    val kind: Kind,
    override val message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    /** 해당 서비스에 활용신청이 되어 있지 않다. 몇 번을 다시 해도 같다. */
    val isNotRegistered: Boolean
        get() = kind == Kind.NOT_REGISTERED

    /** 초당 요청 수를 넘겼다. 잠시 뒤 다시 하면 된다. */
    val isRateLimited: Boolean
        get() = kind == Kind.RATE_LIMITED

    /** 일일 트래픽을 다 썼다. 오늘은 더 호출해도 소용없다. */
    val isQuotaExceeded: Boolean
        get() = kind == Kind.QUOTA_EXCEEDED

    /** 요청은 정상이고 해당 구간에 자료가 없을 뿐이다. 404 와 같이 취급한다. */
    val isNoData: Boolean
        get() = kind == Kind.NO_DATA

    enum class Kind {
        NOT_REGISTERED,
        RATE_LIMITED,
        QUOTA_EXCEEDED,
        NO_DATA,

        /** 위 어느 것도 아닌 실패. 구간 하나의 문제로 보고 요청은 성립한 것으로 친다. */
        OTHER,
    }
}
