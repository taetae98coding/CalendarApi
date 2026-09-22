package io.github.taetae98coding.calendar.domain.holiday

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 공휴일 하나. 연속된 날짜는 [start] 부터 [endInclusive] 까지의 기간으로 표현한다. (예: 설날 3일) */
@Serializable
data class Holiday(
    @SerialName("name")
    val name: String,
    @SerialName("isHoliday")
    val isHoliday: Boolean,
    @SerialName("start")
    val start: LocalDate,
    @SerialName("endInclusive")
    val endInclusive: LocalDate,
)
