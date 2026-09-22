package io.github.taetae98coding.calendar.data.cache

import io.github.taetae98coding.calendar.core.Logger
import io.github.taetae98coding.calendar.core.file.DirectoryPruner
import io.github.taetae98coding.calendar.core.file.DirectoryPruner.Decision
import io.github.taetae98coding.calendar.core.file.YearFileName
import io.github.taetae98coding.calendar.data.source.CacheProvider
import io.github.taetae98coding.calendar.data.source.Granularity
import io.github.taetae98coding.calendar.data.source.SourceApi
import io.github.taetae98coding.calendar.domain.CalendarYears

/**
 * 고정 범위 밖이거나 더는 쓰지 않는 원천의 캐시를 지운다.
 *
 * 트리는 `{provider}/{api}/{year}` 세 단계다. [SourceApi] 에 없는 이름은 어느 단계에서든 지운다.
 * 응답 단위가 그대로 파일 이름이 되어, 월 단위 API 는 연도 폴더(`2026`), 연 단위 API 는 연도 파일(`2026.json`) 만 남는다.
 * 단위가 바뀌면 이전 단위로 쌓인 파일이 여기서 정리된다.
 */
class CachePruner(
    private val paths: CachePaths,
    logger: Logger,
    private val years: IntRange = CalendarYears.years,
) {
    private val pruner = DirectoryPruner(logger)

    suspend fun prune() {
        pruner.prune(paths.root, ::decide)
    }

    private fun decide(path: List<String>): Decision {
        val provider = CacheProvider.entries.find { provider -> provider.id == path[0] } ?: return Decision.DELETE
        if (path.size == 1) return Decision.DESCEND

        val api = SourceApi.of(provider).find { api -> api.id == path[1] } ?: return Decision.DELETE
        if (path.size == 2) return Decision.DESCEND

        val name = path[2]
        if (name == CachePaths.META_FILE_NAME) return Decision.KEEP

        return if (name == api.expectedName(name)) Decision.KEEP else Decision.DELETE
    }

    private fun SourceApi.expectedName(name: String): String? {
        val year = YearFileName.yearOf(name) ?: return null
        if (year !in years) return null

        return when (granularity) {
            Granularity.YEAR -> YearFileName.file(year)
            Granularity.MONTH -> YearFileName.directory(year)
        }
    }
}
