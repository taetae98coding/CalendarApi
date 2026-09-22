package io.github.taetae98coding.calendar.openapi.kasi

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/**
 * 한국천문연구원 음양력 정보(LrsrCldInfoService) 항목.
 *
 * 숫자 필드가 응답에 따라 문자열("01")로도, 숫자(1)로도 내려오기 때문에 [JsonPrimitive] 로 받아 직접 해석한다.
 */
@Serializable
data class KasiLunarItem(
    @SerialName("solYear")
    val solarYear: JsonPrimitive,
    @SerialName("solMonth")
    val solarMonth: JsonPrimitive,
    @SerialName("solDay")
    val solarDay: JsonPrimitive,
    @SerialName("lunYear")
    val lunarYear: JsonPrimitive,
    @SerialName("lunMonth")
    val lunarMonth: JsonPrimitive,
    @SerialName("lunDay")
    val lunarDay: JsonPrimitive,
    @Serializable(KasiLeapMonthSerializer::class)
    @SerialName("lunLeapmonth")
    val isLeapMonth: Boolean,
) {
    val solarDate: LocalDate
        get() = LocalDate(solarYear.toIntValue(), solarMonth.toIntValue(), solarDay.toIntValue())

    val lunarYearValue: Int
        get() = lunarYear.toIntValue()

    val lunarMonthValue: Int
        get() = lunarMonth.toIntValue()

    val lunarDayValue: Int
        get() = lunarDay.toIntValue()

    private fun JsonPrimitive.toIntValue(): Int = content.trim().toInt()
}
