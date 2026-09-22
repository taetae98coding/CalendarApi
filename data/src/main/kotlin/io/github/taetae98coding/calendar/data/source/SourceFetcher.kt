package io.github.taetae98coding.calendar.data.source

import io.github.taetae98coding.calendar.data.cache.CachePeriod
import kotlinx.serialization.json.JsonElement

/**
 * 원천에서 구간 하나를 받아 온다.
 *
 * 갱신 엔진은 예산·순서·동시성만 맡고 어느 원천을 어떻게 부르는지는 여기에 맡긴다.
 * 테스트에서는 네트워크 없이 가짜로 바꿔 끼운다.
 */
interface SourceFetcher {
    /**
     * 성공하면 캐시에 남길 응답 원본, 요청은 정상인데 남길 항목이 없으면 null.
     *
     * null 이어도 갱신은 성공이다. 항목이 없는 응답으로 이미 받아 둔 캐시를 덮어쓰지 않기 위해 원본을 돌려주지 않는 것뿐이다.
     */
    suspend fun fetch(api: SourceApi, period: CachePeriod): Result<JsonElement?>

    /**
     * 본 수집 전에 확인해 지금 부를 수 없는 서비스.
     *
     * 활용신청되지 않은 서비스에 수천 번 호출하는 것을 막는다. 일시적인 실패는 본 수집에서 다시 판단하게 둔다.
     */
    suspend fun unavailable(): Set<FetchService>
}
