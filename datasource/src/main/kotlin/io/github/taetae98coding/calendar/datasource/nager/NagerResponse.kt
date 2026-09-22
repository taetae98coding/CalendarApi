package io.github.taetae98coding.calendar.datasource.nager

import io.github.taetae98coding.calendar.datasource.http.HttpClients
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

/** Nager.Date 응답 원본을 해석한다. 방금 받은 응답이든 캐시에 저장해 둔 원본이든 같은 방식으로 읽는다. */
object NagerResponse {
    fun holidays(raw: JsonElement): List<NagerHoliday> = HttpClients.json.decodeFromJsonElement(raw)
}
