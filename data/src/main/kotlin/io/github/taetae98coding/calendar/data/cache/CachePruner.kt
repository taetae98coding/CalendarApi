package io.github.taetae98coding.calendar.data.cache

import io.github.taetae98coding.calendar.data.Logger
import io.github.taetae98coding.calendar.data.source.CacheProvider
import io.github.taetae98coding.calendar.data.source.Granularity
import io.github.taetae98coding.calendar.data.source.SourceApi
import io.github.taetae98coding.calendar.domain.CalendarYears
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 고정 범위 밖이거나 더는 쓰지 않는 원천의 캐시를 지운다.
 *
 * 범위나 폴더 구조가 바뀌어도 이전 실행이 남긴 파일이 그대로 남지 않도록, 아는 이름만 남기고 나머지는 전부 정리한다.
 */
class CachePruner(
    private val paths: CachePaths,
    private val logger: Logger,
    private val years: IntRange = CalendarYears.years,
) {
    suspend fun prune() {
        withContext(Dispatchers.IO) {
            val providers = CacheProvider.entries.associateBy(CacheProvider::id)

            paths.root.children().forEach { providerDirectory ->
                val provider = providers[providerDirectory.name] ?: return@forEach delete(providerDirectory)
                val apis = SourceApi.of(provider).associateBy(SourceApi::id)

                providerDirectory.children().forEach { apiDirectory ->
                    val api = apis[apiDirectory.name] ?: return@forEach delete(apiDirectory)

                    apiDirectory.children().forEach { child ->
                        if (child.name == CachePaths.META_FILE_NAME) return@forEach
                        if (child.name != api.expectedName(child.name)) delete(child)
                    }
                }
            }
        }
    }

    /**
     * 응답 단위가 그대로 파일 이름이 된다.
     * 월 단위 API 는 연도 폴더(`2026`), 연 단위 API 는 연도 파일(`2026.json`) 만 남는다.
     * 단위가 바뀌면 이전 단위로 쌓인 파일이 여기서 정리된다.
     */
    private fun SourceApi.expectedName(name: String): String? {
        val year = YEAR_REGEX.find(name)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        if (year !in years) return null

        return when (granularity) {
            Granularity.YEAR -> "$year.json"
            Granularity.MONTH -> "$year"
        }
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
