package io.github.taetae98coding.calendar.meta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiMeta(
    @SerialName("updatedAt")
    val updatedAt: String,
    @SerialName("holiday")
    val holiday: HolidayMeta,
    @SerialName("lunar")
    val lunar: LunarMeta,
)

@Serializable
data class HolidayMeta(
    @SerialName("startYear")
    val startYear: Int?,
    @SerialName("endInclusiveYear")
    val endInclusiveYear: Int?,
    @SerialName("countries")
    val countries: List<CountryMeta>,
)

@Serializable
data class CountryMeta(
    @SerialName("code")
    val code: String,
    @SerialName("name")
    val name: String,
    @SerialName("startYear")
    val startYear: Int?,
    @SerialName("endInclusiveYear")
    val endInclusiveYear: Int?,
    @SerialName("sources")
    val sources: List<String>,
)

@Serializable
data class LunarMeta(
    @SerialName("startYear")
    val startYear: Int,
    @SerialName("endInclusiveYear")
    val endInclusiveYear: Int,
    @SerialName("generated")
    val generated: List<YearRange>,
    @SerialName("missing")
    val missing: List<YearRange>,
    @SerialName("source")
    val source: String,
)

@Serializable
data class YearRange(
    @SerialName("start")
    val start: Int,
    @SerialName("endInclusive")
    val endInclusive: Int,
)
