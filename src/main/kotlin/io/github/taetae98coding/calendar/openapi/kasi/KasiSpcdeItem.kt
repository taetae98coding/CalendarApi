package io.github.taetae98coding.calendar.openapi.kasi

import io.github.taetae98coding.calendar.holiday.HolidayName
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 한국천문연구원 특일 정보(SpcdeInfoService) 항목. */
@Serializable
data class KasiSpcdeItem(
    @SerialName("dateKind")
    val kind: KasiSpcdeItemKind,
    @SerialName("dateName")
    val name: String,
    @Serializable(KasiBooleanSerializer::class)
    @SerialName("isHoliday")
    val isHoliday: Boolean,
    @Serializable(KasiLocalDateSerializer::class)
    @SerialName("locdate")
    val date: LocalDate,
) {
    val prettyName: String
        get() = HolidayName.normalize(name)
}
