package io.github.taetae98coding.calendar.data.source

import io.github.taetae98coding.calendar.data.cache.CachePeriod
import io.github.taetae98coding.calendar.datasource.kasi.KasiApi
import io.github.taetae98coding.calendar.domain.Country

/**
 * 캐시가 존재하는 원천 API 한 종류. `cache/{provider}/{id}/` 하나에 대응한다.
 *
 * 여기 없는 원천은 받지도, 남기지도 않는다. 항목이 빠지면 [io.github.taetae98coding.calendar.data.cache.CachePruner] 가 폴더를 지운다.
 */
enum class SourceApi(
    val remote: Remote,
) {
    KASI_REST_DE(Remote.Kasi(KasiApi.REST_DE)),
    KASI_HOLI_DE(Remote.Kasi(KasiApi.HOLI_DE)),
    KASI_ANNIVERSARY(Remote.Kasi(KasiApi.ANNIVERSARY)),
    KASI_24_DIVISIONS(Remote.Kasi(KasiApi.TWENTY_FOUR_DIVISIONS)),
    KASI_SUNDRY_DAY(Remote.Kasi(KasiApi.SUNDRY_DAY)),
    KASI_LUN_CAL(Remote.Kasi(KasiApi.LUN_CAL)),
    NAGER_KOREA(Remote.Nager(Country.KOREA)),
    NAGER_UNITED_STATES(Remote.Nager(Country.UNITED_STATES)),
    ;

    val provider: CacheProvider
        get() = remote.provider

    val id: String
        get() = remote.id

    val service: FetchService
        get() = remote.service

    val granularity: Granularity
        get() = remote.granularity

    /** 고정 범위를 이 API 의 응답 단위로 쪼갠 구간 목록. 캐시 파일 하나, 요청 한 건과 같다. */
    fun periods(years: IntRange): List<CachePeriod> {
        return when (granularity) {
            Granularity.YEAR -> years.map { year -> CachePeriod(year) }
            Granularity.MONTH -> years.flatMap { year -> (1..12).map { month -> CachePeriod(year, month) } }
        }
    }

    companion object {
        /** 특일 정보는 같은 날을 여러 API 가 나눠 들고 있어 전부 읽어야 한다. */
        val spcde: List<SourceApi> = entries.filter { api -> api.service == FetchService.KASI_SPCDE }

        fun nager(country: Country): SourceApi = entries.first { api -> (api.remote as? Remote.Nager)?.country == country }

        fun of(provider: CacheProvider): List<SourceApi> = entries.filter { api -> api.provider == provider }
    }
}
