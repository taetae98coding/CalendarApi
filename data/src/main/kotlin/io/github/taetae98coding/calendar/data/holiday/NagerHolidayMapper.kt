package io.github.taetae98coding.calendar.data.holiday

import io.github.taetae98coding.calendar.datasource.nager.NagerHoliday
import io.github.taetae98coding.calendar.domain.Country
import io.github.taetae98coding.calendar.domain.holiday.Holiday
import io.github.taetae98coding.calendar.domain.holiday.HolidayName

/** @see KasiHolidayMapper */
object NagerHolidayMapper {
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
