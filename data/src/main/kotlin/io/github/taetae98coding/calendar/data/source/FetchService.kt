package io.github.taetae98coding.calendar.data.source

import io.github.taetae98coding.calendar.datasource.kasi.KasiService

/**
 * 트래픽 한도와 중단이 따로 잡히는 단위.
 *
 * 공공데이터포털은 인증키가 하나여도 서비스마다 한도를 따로 세고 활용신청도 따로 해야 한다.
 * Nager.Date 는 인증이 없지만 같은 단위로 묶어 한 곳이 느려도 나머지가 진행되게 한다.
 */
enum class FetchService(
    val displayName: String,
    /** 활용신청이 필요한 서비스만 가진다. 신청되지 않았을 때 사람에게 알려줄 주소다. */
    val applyUrl: String?,
) {
    KASI_SPCDE("한국천문연구원_특일 정보", "https://www.data.go.kr/tcs/dss/selectApiDataDetailView.do?publicDataPk=15012690"),
    KASI_LUNAR("한국천문연구원_음양력 정보", "https://www.data.go.kr/data/15012679/openapi.do"),
    NAGER("Nager.Date", null),
    ;

    companion object {
        fun of(service: KasiService): FetchService {
            return when (service) {
                KasiService.SPCDE -> KASI_SPCDE
                KasiService.LUNAR -> KASI_LUNAR
            }
        }
    }
}
