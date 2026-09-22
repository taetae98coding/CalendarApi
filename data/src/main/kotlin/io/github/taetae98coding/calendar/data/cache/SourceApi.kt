package io.github.taetae98coding.calendar.data.cache

import io.github.taetae98coding.calendar.datasource.openapi.kasi.KasiService
import io.github.taetae98coding.calendar.domain.holiday.Country

/** 캐시 경로의 첫 단계. */
enum class CacheProvider(val id: String) {
    KASI("kasi"),
    NAGER("nager"),
}

/**
 * 트래픽 한도와 예산이 따로 잡히는 단위.
 *
 * 공공데이터포털은 인증키가 하나여도 서비스마다 한도를 따로 센다.
 * Nager.Date 는 인증이 없지만 같은 방식으로 예산을 걸어 한 곳이 느려도 나머지가 진행되게 한다.
 */
enum class FetchService(val kasiService: KasiService?) {
    KASI_SPCDE(KasiService.SPCDE),
    KASI_LUNAR(KasiService.LUNAR),
    NAGER(null),
}

/** 요청 한 건이 덮는 구간. 응답 단위를 그대로 따른다. */
enum class Granularity {
    YEAR,
    MONTH,
}

/**
 * 캐시가 존재하는 원천 API 한 종류.
 *
 * [id] 가 `cache/{provider}/{id}/` 까지를 만들고, 그 아래는 [granularity] 가 정한다.
 * Nager.Date 는 국가별로 엔드포인트가 갈리므로 국가를 [id] 에 포함한다.
 */
enum class SourceApi(
    val provider: CacheProvider,
    val id: String,
    val service: FetchService,
    val granularity: Granularity,
    val country: Country? = null,
) {
    KASI_REST_DE(CacheProvider.KASI, "getRestDeInfo", FetchService.KASI_SPCDE, Granularity.MONTH),
    KASI_HOLI_DE(CacheProvider.KASI, "getHoliDeInfo", FetchService.KASI_SPCDE, Granularity.MONTH),
    KASI_ANNIVERSARY(CacheProvider.KASI, "getAnniversaryInfo", FetchService.KASI_SPCDE, Granularity.MONTH),
    KASI_24_DIVISIONS(CacheProvider.KASI, "get24DivisionsInfo", FetchService.KASI_SPCDE, Granularity.MONTH),
    KASI_SUNDRY_DAY(CacheProvider.KASI, "getSundryDayInfo", FetchService.KASI_SPCDE, Granularity.MONTH),
    KASI_LUN_CAL(CacheProvider.KASI, "getLunCalInfo", FetchService.KASI_LUNAR, Granularity.MONTH),
    NAGER_KOREA(CacheProvider.NAGER, "publicHolidays-kr", FetchService.NAGER, Granularity.YEAR, Country.KOREA),
    NAGER_UNITED_STATES(CacheProvider.NAGER, "publicHolidays-us", FetchService.NAGER, Granularity.YEAR, Country.UNITED_STATES),
    ;

    fun periods(years: IntRange): List<CachePeriod> {
        return when (granularity) {
            Granularity.YEAR -> years.map { year -> CachePeriod(year) }
            Granularity.MONTH -> years.flatMap { year -> (1..12).map { month -> CachePeriod(year, month) } }
        }
    }

    companion object {
        /** 특일 정보는 같은 날을 여러 엔드포인트가 나눠 들고 있어 전부 읽어야 한다. */
        val spcde: List<SourceApi> = entries.filter { api -> api.service == FetchService.KASI_SPCDE }

        fun nager(country: Country): SourceApi = entries.first { api -> api.provider == CacheProvider.NAGER && api.country == country }
    }
}
