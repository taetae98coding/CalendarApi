package io.github.taetae98coding.calendar.data.lunar

import io.github.taetae98coding.calendar.datasource.kasi.KasiLunarItem
import io.github.taetae98coding.calendar.domain.lunar.LunarDate

/** 음양력 정보 응답을 도메인 모델로 옮긴다. */
object LunarMapper {
    fun toLunarDate(item: KasiLunarItem): LunarDate {
        return LunarDate(
            solar = item.solarDate,
            year = item.lunarYearValue,
            month = item.lunarMonthValue,
            day = item.lunarDayValue,
            isLeapMonth = item.isLeapMonth,
        )
    }
}
