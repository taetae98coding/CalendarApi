package io.github.taetae98coding.calendar.data.source

import io.github.taetae98coding.calendar.datasource.kasi.KasiApi
import io.github.taetae98coding.calendar.domain.Country

/**
 * 요청이 어디로 나가는가.
 *
 * 캐시 경로([provider], [id]), 중단 단위([service]), 구간 단위([granularity])가 전부 여기서 결정된다.
 * 원천을 부르는 쪽은 타입으로 분기하므로 "KASI 인데 국가가 없다" 같은 nullable 조합이 생기지 않는다.
 */
sealed interface Remote {
    val provider: CacheProvider
    val id: String
    val service: FetchService
    val granularity: Granularity

    /** 한국천문연구원. API 하나가 캐시 폴더 하나다. 월 단위로 응답한다. */
    data class Kasi(
        val api: KasiApi,
    ) : Remote {
        override val provider: CacheProvider = CacheProvider.KASI
        override val id: String = api.path
        override val service: FetchService = FetchService.of(api.service)
        override val granularity: Granularity = Granularity.MONTH
    }

    /** Nager.Date. 국가별로 엔드포인트가 갈리므로 국가가 캐시 폴더를 만든다. 연 단위로 응답한다. */
    data class Nager(
        val country: Country,
    ) : Remote {
        /** Nager.Date 는 ISO 3166-1 alpha-2 대문자를 쓴다. */
        val countryCode: String = country.code.uppercase()

        override val provider: CacheProvider = CacheProvider.NAGER
        override val id: String = "publicHolidays-${country.code}"
        override val service: FetchService = FetchService.NAGER
        override val granularity: Granularity = Granularity.YEAR
    }
}
