package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.domain.Country
import io.github.taetae98coding.calendar.publish.DocApi
import io.github.taetae98coding.calendar.publish.SourceCatalog

/**
 * 어느 API 의 어느 국가 자료가 어디서 왔는지.
 *
 * 저장소 구현([io.github.taetae98coding.calendar.data.holiday.CachedHolidayRepository],
 * [io.github.taetae98coding.calendar.data.lunar.CachedLunarRepository])이 실제로 쓰는 원천과 같아야 한다.
 * 저장소에 원천을 붙이는 곳이 여기([CalendarApp])라 설명도 여기서 관리한다.
 */
object CalendarSources : SourceCatalog {
    private const val KASI_SPCDE = "한국천문연구원 특일 정보 (공공데이터포털)"
    private const val KASI_LUNAR = "한국천문연구원 음양력 정보 (공공데이터포털)"
    private const val NAGER = "Nager.Date"
    private const val NAGER_FALLBACK = "Nager.Date (특일 정보에 공휴일이 없는 연도 보완)"

    override fun sources(api: DocApi, country: Country): List<String> {
        val holiday = when (country) {
            Country.KOREA -> listOf(KASI_SPCDE, NAGER_FALLBACK)
            Country.UNITED_STATES -> listOf(NAGER)
        }

        return when (api) {
            DocApi.HOLIDAY -> holiday
            DocApi.LUNAR -> listOf(KASI_LUNAR)
            DocApi.CALENDAR -> holiday + KASI_LUNAR
        }
    }
}
