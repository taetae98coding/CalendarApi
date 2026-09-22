package io.github.taetae98coding.calendar.data.holiday

import io.github.taetae98coding.calendar.datasource.openapi.kasi.KasiSpcdeItem
import io.github.taetae98coding.calendar.datasource.openapi.nager.NagerHoliday
import io.github.taetae98coding.calendar.domain.holiday.Country
import io.github.taetae98coding.calendar.domain.holiday.Holiday
import io.github.taetae98coding.calendar.domain.holiday.HolidayName

/**
 * 출처의 표기를 도메인 어휘로 옮긴다.
 *
 * 같은 공휴일이 출처마다 다른 이름으로 내려오므로([HolidayName]), 그 차이를 흡수하는 자리를 출처별로 하나씩 둔다.
 * 원천 DTO 는 응답에 적힌 표기를 그대로 들고 있고, 이름을 맞추는 일은 전부 여기서 일어난다.
 */
data object KasiHolidayMapper {
    fun toHoliday(item: KasiSpcdeItem): Holiday {
        return Holiday(
            name = HolidayName.normalize(item.name),
            isHoliday = item.isHoliday,
            start = item.date,
            endInclusive = item.date,
        )
    }
}

/** @see KasiHolidayMapper */
data object NagerHolidayMapper {
    /** 지역 한정 공휴일(`counties`)은 전국 공휴일이 아니라 내보내지 않는다. */
    fun toHolidays(items: List<NagerHoliday>, country: Country): List<Holiday> {
        return items.filter { item -> item.counties == null }
            .map { item -> toHoliday(item, country) }
    }

    private fun toHoliday(item: NagerHoliday, country: Country): Holiday {
        return Holiday(
            // 한국 공휴일만 특일 정보와 이름을 맞춘다. 다른 나라는 맞출 상대가 없어 현지 표기를 그대로 쓴다.
            name = if (country == Country.KOREA) HolidayName.normalize(item.localName) else item.localName,
            isHoliday = item.isPublicHoliday && item.isGlobal,
            start = item.date,
            endInclusive = item.date,
        )
    }
}
