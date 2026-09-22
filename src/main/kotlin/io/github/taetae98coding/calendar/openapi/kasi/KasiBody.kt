package io.github.taetae98coding.calendar.openapi.kasi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class KasiBody(
    @SerialName("items")
    val items: JsonElement,
    @SerialName("numOfRows")
    val row: Int = 0,
    @SerialName("pageNo")
    val page: Int = 0,
    @SerialName("totalCount")
    val count: Int,
)

@Serializable
data class KasiSingleItems<T>(
    @SerialName("item")
    val item: T,
)

@Serializable
data class KasiMultiItems<T>(
    @SerialName("item")
    val item: List<T>,
)

/**
 * 공공데이터포털은 결과가 1건이면 items.item 을 객체로, 2건 이상이면 배열로 내려준다.
 * 0건이면 items 가 빈 문자열이 되기도 하므로 totalCount 로 먼저 분기한다.
 */
inline fun <reified T> KasiBody.toItemList(json: Json): List<T> {
    return when (count) {
        0 -> emptyList()
        1 -> listOf(json.decodeFromJsonElement<KasiSingleItems<T>>(items).item)
        else -> json.decodeFromJsonElement<KasiMultiItems<T>>(items).item
    }
}
