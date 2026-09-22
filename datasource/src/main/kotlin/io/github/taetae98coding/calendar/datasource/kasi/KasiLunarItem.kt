package io.github.taetae98coding.calendar.datasource.kasi

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
    @SerialName("solJd")
    val solarJulianDay: JsonPrimitive? = null,
) {
    /**
     * 양력 날짜.
     *
     * KASI 는 그레고리력 도입(1582-10-15) 이전 구간을 율리우스력으로 준다.
     * 율리우스력에서는 윤년이던 1500-02-29 같은 날짜가 있어 solYear/solMonth/solDay 를 그대로
     * [LocalDate] 로 만들면 실패한다. 달력 종류와 무관한 율리우스 적일(solJd)로 만들어 ISO-8601
     * (proleptic Gregorian) 날짜로 통일한다.
     */
    val solarDate: LocalDate
        get() = solarJulianDay?.let { julianDay -> LocalDate.fromEpochDays(julianDay.toIntValue() - JULIAN_DAY_OF_EPOCH) }
            ?: LocalDate(solarYear.toIntValue(), solarMonth.toIntValue(), solarDay.toIntValue())

    val lunarYearValue: Int
        get() = lunarYear.toIntValue()

    val lunarMonthValue: Int
        get() = lunarMonth.toIntValue()

    val lunarDayValue: Int
        get() = lunarDay.toIntValue()

    private fun JsonPrimitive.toIntValue(): Int = content.trim().toInt()

    companion object {
        /** 1970-01-01 의 율리우스 적일. */
        private const val JULIAN_DAY_OF_EPOCH = 2440588
    }
}
