package io.github.taetae98coding.calendar.meta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiMeta(
    @SerialName("updatedAt")
    val updatedAt: String,
    /** 모든 API 가 공통으로 제공하는 고정 범위. */
    @SerialName("startYear")
    val startYear: Int,
    @SerialName("endInclusiveYear")
    val endInclusiveYear: Int,
    @SerialName("apis")
    val apis: List<ApiCoverage>,
)

@Serializable
data class ApiCoverage(
    @SerialName("id")
    val id: String,
    @SerialName("countries")
    val countries: List<CountryCoverage>,
)

@Serializable
data class CountryCoverage(
    @SerialName("code")
    val code: String,
    @SerialName("name")
    val name: String,
    /** 실제로 파일이 만들어진 연도. 고정 범위 안이어도 원천에 자료가 없으면 비어 있을 수 있다. */
    @SerialName("generated")
    val generated: List<YearRange>,
    @SerialName("missing")
    val missing: List<YearRange>,
    @SerialName("sources")
    val sources: List<String>,
)

@Serializable
data class YearRange(
    @SerialName("start")
    val start: Int,
    @SerialName("endInclusive")
    val endInclusive: Int,
)
