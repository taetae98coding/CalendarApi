package io.github.taetae98coding.calendar.openapi.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenApiResult<T>(
    @SerialName("response")
    val response: OpenApiResponse<T>,
)
