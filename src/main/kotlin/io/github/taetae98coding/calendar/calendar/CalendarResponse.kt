package io.github.taetae98coding.calendar.calendar

import io.github.taetae98coding.calendar.holiday.Holiday
import io.github.taetae98coding.calendar.lunar.LunarDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 공휴일과 음력을 한 번에 내려주는 통합 응답. */
@Serializable
data class CalendarResponse(
    @SerialName("country")
    val country: String,
    @SerialName("period")
    val period: String,
    @SerialName("holidays")
    val holidays: List<Holiday>,
    @SerialName("lunar")
    val lunar: List<LunarDate>,
)
