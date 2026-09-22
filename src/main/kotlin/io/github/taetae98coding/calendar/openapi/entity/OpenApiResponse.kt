package io.github.taetae98coding.calendar.openapi.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenApiResponse<T>(
    @SerialName("header")
    val header: OpenApiHeader,
    @SerialName("body")
    val body: T,
)
