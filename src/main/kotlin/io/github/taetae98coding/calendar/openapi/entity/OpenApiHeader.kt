package io.github.taetae98coding.calendar.openapi.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenApiHeader(
    @SerialName("resultCode")
    val code: String,
    @SerialName("resultMsg")
    val message: String,
)
