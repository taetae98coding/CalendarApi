package io.github.taetae98coding.calendar.datasource.openapi.nager

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** https://date.nager.at 의 PublicHolidays 응답 항목. */
@Serializable
data class NagerHoliday(
    @SerialName("date")
    val date: LocalDate,
    @SerialName("localName")
    val localName: String,
    @SerialName("name")
    val name: String,
    @SerialName("global")
    val isGlobal: Boolean = true,
    @SerialName("counties")
    val counties: List<String>? = null,
    @SerialName("types")
    val types: List<String> = emptyList(),
) {
    /** Public / Bank 이 아닌 Observance, School 등은 실제 휴무일이 아니다. */
    val isPublicHoliday: Boolean
        get() = types.contains("Public")
}
