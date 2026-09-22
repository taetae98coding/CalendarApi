package io.github.taetae98coding.calendar.openapi.kasi

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
        get() = name.replace("1월1일", "신정")
            .replace("기독탄신일", "크리스마스")
            .replace("대체공휴일(설날)", "설날")
            .replace("대체공휴일(추석)", "추석")
            .replace("임시공휴일\\((.*?)\\)".toRegex(), "$1")
}
