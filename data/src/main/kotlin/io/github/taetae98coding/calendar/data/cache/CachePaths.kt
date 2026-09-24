package io.github.taetae98coding.calendar.data.cache

import io.github.taetae98coding.calendar.data.source.CacheProvider
import io.github.taetae98coding.calendar.data.source.SourceApi
import java.io.File

/**
 * 캐시 경로 규칙. `cache/{provider}/{api}/{year}/{month}.json` (연 단위 응답은 `cache/{provider}/{api}/{year}.json`)
 *
 * 배포 경로는 `:publish` 의 DocPaths 가 따로 관리한다. 캐시는 수집의 사정, 배포는 공개 규격이라 같이 바뀌지 않는다.
 */
class CachePaths(
    val root: File,
) {
    fun provider(provider: CacheProvider): File = File(root, provider.id)

    fun api(api: SourceApi): File = File(provider(api.provider), api.id)

    /** 구간 하나의 응답 원본. 월 단위 API 는 `{year}/{month}.json`, 연 단위 API 는 `{year}.json`. */
    fun file(api: SourceApi, period: CachePeriod): File = File(api(api), "${period.key}.json")

    /** API 하나의 구간별 마지막 갱신 시각. 월 파일·연 파일과 이름이 겹치지 않는다. */
    fun meta(api: SourceApi): File = File(api(api), META_FILE_NAME)

    companion object {
        const val META_FILE_NAME = "meta.json"
    }
}
