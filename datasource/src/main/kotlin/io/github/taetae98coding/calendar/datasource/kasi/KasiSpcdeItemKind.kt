package io.github.taetae98coding.calendar.datasource.kasi

import kotlinx.serialization.Serializable

/** 특일 정보의 `dateKind`. 어느 API 가 내려준 항목인지 알려준다. */
@Serializable
@JvmInline
value class KasiSpcdeItemKind(
    val kind: String,
) {
    companion object {
        val NationalHoliday = KasiSpcdeItemKind("01")
        val Anniversary = KasiSpcdeItemKind("02")
        val TwentyFourSolarTerms = KasiSpcdeItemKind("03")
        val MiscellaneousHoliday = KasiSpcdeItemKind("04")
    }
}
