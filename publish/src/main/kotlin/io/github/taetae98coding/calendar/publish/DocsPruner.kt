package io.github.taetae98coding.calendar.publish

import io.github.taetae98coding.calendar.core.Logger
import io.github.taetae98coding.calendar.core.file.DirectoryPruner
import io.github.taetae98coding.calendar.core.file.DirectoryPruner.Decision
import io.github.taetae98coding.calendar.core.file.YearFileName
import io.github.taetae98coding.calendar.domain.CalendarYears
import io.github.taetae98coding.calendar.domain.Country

/**
 * 고정 범위 밖의 배포 문서를 지운다.
 *
 * 트리는 `{api}/{country}/{year}` 세 단계고 루트에는 [DocPaths.rootFiles] 가 함께 있다.
 * 연도 파일(`2026.json`)과 월 폴더(`2026/`)가 같은 자리에 있으므로 둘 다 이름의 연도로 판단한다.
 */
class DocsPruner(
    private val paths: DocPaths,
    logger: Logger,
    private val years: IntRange = CalendarYears.years,
) {
    private val pruner = DirectoryPruner(logger)

    suspend fun prune() {
        pruner.prune(paths.root, ::decide)
    }

    private fun decide(path: List<String>): Decision {
        return when (path.size) {
            1 -> when {
                path[0] in DocPaths.rootFiles -> Decision.KEEP
                DocApi.entries.any { api -> api.id == path[0] } -> Decision.DESCEND
                else -> Decision.DELETE
            }

            2 -> if (Country.entries.any { country -> country.code == path[1] }) Decision.DESCEND else Decision.DELETE

            else -> if (YearFileName.yearOf(path[2]) in years) Decision.KEEP else Decision.DELETE
        }
    }
}
