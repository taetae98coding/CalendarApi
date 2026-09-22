package io.github.taetae98coding.calendar.datasource.kasi

/**
 * 한국천문연구원 OpenAPI 하나. [path] 가 그대로 요청 경로의 마지막 조각이다.
 *
 * 모두 양력 연·월을 받아 그 달의 항목을 돌려준다. 어느 [service] 에 속하는지가 활용신청과 한도의 단위를 정한다.
 */
enum class KasiApi(
    val service: KasiService,
    val path: String,
) {
    /** 공휴일. */
    REST_DE(KasiService.SPCDE, "getRestDeInfo"),

    /** 국경일. */
    HOLI_DE(KasiService.SPCDE, "getHoliDeInfo"),

    /** 기념일. */
    ANNIVERSARY(KasiService.SPCDE, "getAnniversaryInfo"),

    /** 24절기. */
    TWENTY_FOUR_DIVISIONS(KasiService.SPCDE, "get24DivisionsInfo"),

    /** 잡절. */
    SUNDRY_DAY(KasiService.SPCDE, "getSundryDayInfo"),

    /** 양력 날짜별 음력. */
    LUN_CAL(KasiService.LUNAR, "getLunCalInfo"),
    ;

    /** 요청 경로. `{service}/{api}` */
    val route: String
        get() = "${service.path}/$path"
}
