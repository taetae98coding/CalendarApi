package io.github.taetae98coding.calendar.data.cache

import kotlinx.serialization.json.JsonElement

/**
 * 도메인 하나가 자기 원천을 어떻게 받는지.
 *
 * [UpdateScheduler] 는 예산·순서·동시성만 맡고, 어느 원천을 어떻게 부르는지는 모른다.
 * 출처가 늘어도 갱신 엔진이 아니라 해당 도메인의 구현만 바뀐다.
 */
interface SourceUpdater {
    /** 로그에 찍히는 도메인 이름. */
    val subject: String

    /** 이 도메인이 책임지는 원천. 한 도메인이 여러 출처를 쓸 수 있다. (예: 공휴일 = 특일 정보 + Nager.Date) */
    val apis: List<SourceApi>

    /** 성공하면 캐시에 남길 응답 원본, 요청은 정상인데 자료가 없으면 null. */
    suspend fun fetch(unit: CacheUnit): Result<JsonElement?>

    /**
     * 응답에 항목이 들어 있는가.
     *
     * 일시적인 빈 응답으로 이미 받아 둔 캐시를 잃지 않기 위해 쓴다.
     * 응답 구조는 출처마다 다르므로 그 출처를 아는 도메인이 판단한다.
     */
    fun hasItems(api: SourceApi, raw: JsonElement): Boolean
}
