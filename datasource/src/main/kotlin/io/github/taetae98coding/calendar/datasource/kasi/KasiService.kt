package io.github.taetae98coding.calendar.datasource.kasi

/**
 * 한국천문연구원이 공공데이터포털에 올린 서비스. 인증키는 계정당 하나지만 활용신청은 서비스 단위다.
 *
 * [probeApi] 는 활용신청 여부를 확인할 때 찔러 보는 API 다. 서비스가 제공하는 것이면 무엇이든 된다.
 */
enum class KasiService(
    val path: String,
    val probeApi: String,
) {
    /** 특일 정보. 공휴일·기념일·24절기·잡절을 API 별로 나눠 제공한다. */
    SPCDE(path = "SpcdeInfoService", probeApi = "getRestDeInfo"),

    /** 음양력 정보. */
    LUNAR(path = "LrsrCldInfoService", probeApi = "getLunCalInfo"),
}
