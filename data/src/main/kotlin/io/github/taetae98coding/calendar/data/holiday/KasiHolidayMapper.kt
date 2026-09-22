package io.github.taetae98coding.calendar.data.holiday

import io.github.taetae98coding.calendar.datasource.kasi.KasiSpcdeItem
import io.github.taetae98coding.calendar.domain.holiday.Holiday
import io.github.taetae98coding.calendar.domain.holiday.HolidayName

/**
 * 출처의 표기를 도메인 어휘로 옮긴다.
 *
 * 같은 공휴일이 출처마다 다른 이름으로 내려오므로([HolidayName]), 그 차이를 흡수하는 자리를 출처별로 하나씩 둔다.
 * 원천 DTO 는 응답에 적힌 표기를 그대로 들고 있고, 이름을 맞추는 일은 전부 여기서 일어난다.
 */
object KasiHolidayMapper {
    fun toHoliday(item: KasiSpcdeItem): Holiday {
        return Holiday(
            name = HolidayName.normalize(item.name),
            isHoliday = item.isHoliday,
            start = item.date,
            endInclusive = item.date,
        )
    }
}
