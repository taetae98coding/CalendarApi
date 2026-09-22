package io.github.taetae98coding.calendar.lunar

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 양력 하루에 대응하는 음력 정보.
 *
 * 음력은 윤달이 존재하고 월의 크기가 29/30 일로 달라 [LocalDate] 로 표현할 수 없으므로 연·월·일을 분리해 담는다.
 */
@Serializable
data class LunarDate(
    @SerialName("solar")
    val solar: LocalDate,
    @SerialName("year")
    val year: Int,
    @SerialName("month")
    val month: Int,
    @SerialName("day")
    val day: Int,
    @SerialName("isLeapMonth")
    val isLeapMonth: Boolean,
)
