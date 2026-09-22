package io.github.taetae98coding.calendar.openapi.kasi

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class KasiSpcdeItemKind(
    val kind: String,
) {
    companion object {
        val None = KasiSpcdeItemKind("")
        val NationalHoliday = KasiSpcdeItemKind("01")
        val Anniversary = KasiSpcdeItemKind("02")
        val TwentyFourSolarTerms = KasiSpcdeItemKind("03")
        val MiscellaneousHoliday = KasiSpcdeItemKind("04")
    }
}
