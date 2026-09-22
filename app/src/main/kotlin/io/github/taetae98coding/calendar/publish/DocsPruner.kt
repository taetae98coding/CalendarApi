package io.github.taetae98coding.calendar.publish

import io.github.taetae98coding.calendar.data.Logger
import io.github.taetae98coding.calendar.domain.CalendarYears
import io.github.taetae98coding.calendar.domain.Country
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 고정 범위 밖의 배포 문서를 지운다.
 *
 * 범위나 폴더 구조가 바뀌어도 이전 실행이 남긴 파일이 그대로 배포되지 않도록, 아는 이름만 남기고 나머지는 전부 정리한다.
 */
class DocsPruner(
    private val paths: DocPaths,
    private val logger: Logger,
    private val years: IntRange = CalendarYears.years,
) {
    suspend fun prune() {
        withContext(Dispatchers.IO) {
            val apis = DocApi.entries.map(DocApi::id).toSet()
            val countries = Country.entries.map(Country::code).toSet()

            paths.root.children().forEach { apiDirectory ->
                if (apiDirectory.name in DocPaths.rootFiles) return@forEach
                if (apiDirectory.name !in apis) return@forEach delete(apiDirectory)

                apiDirectory.children().forEach { countryDirectory ->
                    if (countryDirectory.name !in countries) return@forEach delete(countryDirectory)

                    // 연도 파일(2026.json)과 월 폴더(2026/)가 같은 자리에 있으므로 둘 다 이름으로 판단한다.
                    countryDirectory.children().forEach { child -> if (!child.isInRange()) delete(child) }
                }
            }
        }
    }

    private fun File.isInRange(): Boolean {
        val year = YEAR_REGEX.find(name)?.groupValues?.get(1)?.toIntOrNull() ?: return false

        return year in years
    }

    private fun File.children(): List<File> = listFiles().orEmpty().toList()

    private fun delete(file: File) {
        logger.log("[Prune] ${file.path} 삭제")
        file.deleteRecursively()
    }

    companion object {
        private val YEAR_REGEX = "^(\\d{4})(\\.json)?$".toRegex()
    }
}
