package io.github.taetae98coding.calendar.datasource.openapi.kasi

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 한국천문연구원 특일 정보(SpcdeInfoService) 항목.
 *
 * [name] 은 응답에 적힌 표기 그대로다. 출처마다 다른 표기를 하나로 맞추는 일은 `:data` 의 매퍼가 한다.
 */
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
)
