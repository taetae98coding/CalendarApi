package io.github.taetae98coding.calendar.datasource.nager

import kotlinx.serialization.json.JsonElement

/** Nager.Date 공개 공휴일 API. 인증키가 필요 없다. */
interface NagerDataSource {
    /**
     * 성공이면 응답 원본, 요청은 정상인데 남길 항목이 없으면 null.
     *
     * 제공하지 않는 연도(404)와 빈 배열은 모두 "요청은 정상이고 항목이 없을 뿐"이라 성공으로 본다.
     * [countryCode] 는 ISO 3166-1 alpha-2 대문자다.
     */
    suspend fun getHolidays(year: Int, countryCode: String): Result<JsonElement?>
}
