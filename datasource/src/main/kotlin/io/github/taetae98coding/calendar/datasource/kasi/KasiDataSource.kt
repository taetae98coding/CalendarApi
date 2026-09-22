package io.github.taetae98coding.calendar.datasource.kasi

import kotlinx.datetime.YearMonth
import kotlinx.serialization.json.JsonElement

/**
 * 한국천문연구원 OpenAPI.
 *
 * 응답을 도메인 모델로 좁히지 않고 [JsonElement] 그대로 돌려준다.
 * 호출자가 원본을 캐시에 남기고, 필요한 필드만 뽑는 일은 [KasiResponse] 로 따로 한다.
 */
interface KasiDataSource {
    /**
     * 성공이면 응답 원본, 요청은 정상인데 남길 항목이 없으면 null.
     *
     * 404, 자료 없음(code=03), 항목 0건은 모두 "요청은 정상적으로 처리됐고 항목이 없을 뿐"이라 성공으로 본다.
     * 존재하지 않는 범위를 요청해도 갱신 시각은 찍혀야 순번이 돌아간다.
     * `solDay` 를 생략하므로 해당 양력 월 전체를 한 번에 받는다.
     */
    suspend fun get(service: KasiService, api: String, yearMonth: YearMonth): Result<JsonElement?>

    /** 해당 서비스에 활용신청이 되어 있는가. 활용신청 문제가 아닌 일시적 실패는 신청된 것으로 본다. */
    suspend fun isRegistered(service: KasiService): Boolean
}
