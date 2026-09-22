package io.github.taetae98coding.calendar.datasource.openapi.kasi

/**
 * 공공데이터포털 인증키는 계정당 하나지만 활용신청은 서비스 단위로 해야 한다.
 * 어느 서비스가 신청되지 않았는지 알려주기 위해 신청 주소를 함께 들고 있는다.
 */
enum class KasiService(
    val path: String,
    val displayName: String,
    val applyUrl: String,
) {
    SPCDE(
        path = "SpcdeInfoService",
        displayName = "한국천문연구원_특일 정보",
        applyUrl = "https://www.data.go.kr/tcs/dss/selectApiDataDetailView.do?publicDataPk=15012690",
    ),
    LUNAR(
        path = "LrsrCldInfoService",
        displayName = "한국천문연구원_음양력 정보",
        applyUrl = "https://www.data.go.kr/data/15012679/openapi.do",
    ),
}
