package io.github.taetae98coding.calendar.data.holiday

import io.github.taetae98coding.calendar.data.source.SourceApi
import io.github.taetae98coding.calendar.datasource.kasi.KasiSpcdeItem
import io.github.taetae98coding.calendar.datasource.nager.NagerHoliday
import io.github.taetae98coding.calendar.domain.holiday.Holiday

/**
 * 모든 공휴일 출처가 거치는 매퍼. 출처의 표기를 도메인 어휘로 옮긴다.
 *
 * 1. 출처별 진입점([fromKasi] · [fromNager])은 DTO 의 필드만 옮긴다. 응답 모양이 달라 이 단계만 출처마다 다르다.
 * 2. [common] 은 모든 API 의 결과에 똑같이 적용된다. 같은 공휴일이 출처마다 다른 이름으로 내려오는 차이([HolidayName])를 여기서 흡수한다.
 * 3. [customizations] 는 공통 결과를 API 별로 고친다. 등록되지 않은 API 는 공통 결과를 그대로 쓴다.
 *
 * API 가 새로 생기면 공통 처리는 따로 손대지 않아도 적용되고, 다르게 다뤄야 할 때만 [customizations] 에 더한다.
 * 원천 DTO 는 응답에 적힌 표기를 그대로 들고 있고, 이름을 맞추는 일은 전부 여기서 일어난다.
 */
object HolidayMapper {
    /** API 별 후처리. [common] 을 거친 결과를 받는다. */
    private val customizations: Map<SourceApi, (Holiday) -> Holiday> = emptyMap()

    fun fromKasi(api: SourceApi, items: List<KasiSpcdeItem>): List<Holiday> {
        return map(api, items.map { item -> Holiday(item.name, item.isHoliday, item.date, item.date) })
    }

    /** 지역 한정 공휴일(`counties`)은 전국 공휴일이 아니라 내보내지 않는다. */
    fun fromNager(api: SourceApi, items: List<NagerHoliday>): List<Holiday> {
        return map(
            api = api,
            holidays = items.filter { item -> item.counties == null }
                .map { item -> Holiday(item.localName, item.isPublicHoliday && item.isGlobal, item.date, item.date) },
        )
    }

    private fun map(api: SourceApi, holidays: List<Holiday>): List<Holiday> {
        val customize = customizations[api] ?: { holiday -> holiday }

        return holidays.map(::common).map(customize)
    }

    /** 한국어 표기만 규칙에 걸리므로 다른 나라 이름은 공백 정리 외에는 바뀌지 않는다. */
    private fun common(holiday: Holiday): Holiday {
        return holiday.copy(name = HolidayName.normalize(holiday.name))
    }
}
