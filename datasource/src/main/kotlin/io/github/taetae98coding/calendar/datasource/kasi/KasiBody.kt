package io.github.taetae98coding.calendar.datasource.kasi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * 특일 정보와 음양력 정보가 공유하는 본문.
 *
 * 공공데이터포털은 결과가 1건이면 `items.item` 을 객체로, 2건 이상이면 배열로 내려준다.
 * 0건이면 `items` 가 빈 문자열이 되기도 하므로 [count] 로 먼저 분기한다.
 */
@Serializable
data class KasiBody(
    @SerialName("items")
    val items: JsonElement,
    @SerialName("totalCount")
    val count: Int,
) {
    inline fun <reified T> items(json: Json): List<T> {
        return when (count) {
            0 -> emptyList()
            1 -> listOf(json.decodeFromJsonElement<Single<T>>(items).item)
            else -> json.decodeFromJsonElement<Multi<T>>(items).item
        }
    }

    @Serializable
    data class Single<T>(
        @SerialName("item")
        val item: T,
    )

    @Serializable
    data class Multi<T>(
        @SerialName("item")
        val item: List<T>,
    )
}
