package io.github.taetae98coding.calendar.publish

import io.github.taetae98coding.calendar.data.cache.CachePaths
import io.github.taetae98coding.calendar.data.cache.CacheProvider
import io.github.taetae98coding.calendar.data.cache.Granularity
import io.github.taetae98coding.calendar.data.cache.SourceApi
import io.github.taetae98coding.calendar.domain.CalendarYears
import io.github.taetae98coding.calendar.domain.holiday.Country
import io.github.taetae98coding.calendar.publish.DocApi
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 고정 범위([CalendarYears.years]) 밖의 캐시와 배포 문서를 지운다.
 *
 * 범위나 폴더 구조가 바뀌어도 이전 실행이 남긴 파일이 그대로 배포되지 않도록,
 * 아는 이름만 남기고 나머지는 전부 정리한다.
 */
data object Pruner {
    private const val META = "meta.json"

    /** docs 루트에서 API 폴더가 아니어도 남겨야 하는 파일. */
    private val docsKeep = setOf(META, "index.html", ".nojekyll")

    private val yearRegex = "^(\\d{4})(\\.json)?$".toRegex()

    suspend fun prune() {
        withContext(Dispatchers.IO) {
            pruneCache()
            pruneDocs()
        }
    }

    private fun pruneCache() {
        val providers = CacheProvider.entries.associateBy(CacheProvider::id)

        CachePaths.root.children().forEach { providerDirectory ->
            val provider = providers[providerDirectory.name] ?: return@forEach providerDirectory.deleteRecursivelyLogging()
            val apis = SourceApi.entries.filter { api -> api.provider == provider }.associateBy(SourceApi::id)

            providerDirectory.children().forEach { apiDirectory ->
                val api = apis[apiDirectory.name] ?: return@forEach apiDirectory.deleteRecursivelyLogging()

                apiDirectory.children().forEach { child ->
                    if (child.name == META) return@forEach

                    child.pruneUnless(api.expectedName(child.name))
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
        val year = yearRegex.find(name)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        if (year !in CalendarYears.years) return null

        return when (granularity) {
            Granularity.YEAR -> "$year.json"
            Granularity.MONTH -> "$year"
        }
    }

    private fun pruneDocs() {
        val apis = DocApi.entries.associateBy(DocApi::id)
        val countries = Country.entries.map(Country::code).toSet()

        DocPaths.root.children().forEach { apiDirectory ->
            if (apiDirectory.name in docsKeep) return@forEach
            if (apiDirectory.name !in apis) return@forEach apiDirectory.deleteRecursivelyLogging()

            apiDirectory.children().forEach { countryDirectory ->
                if (countryDirectory.name !in countries) return@forEach countryDirectory.deleteRecursivelyLogging()

                // 연도 파일(2026.json)과 월 폴더(2026/)가 같은 자리에 있으므로 둘 다 이름으로 판단한다.
                countryDirectory.children().forEach { child -> child.pruneUnless(child.expectedDocName()) }
            }
        }
    }

    private fun File.expectedDocName(): String? {
        val year = yearRegex.find(name)?.groupValues?.get(1)?.toIntOrNull() ?: return null

        return name.takeIf { year in CalendarYears.years }
    }

    private fun File.pruneUnless(expected: String?) {
        if (expected != name) deleteRecursivelyLogging()
    }

    private fun File.children(): List<File> = listFiles().orEmpty().toList()

    private fun File.deleteRecursivelyLogging() {
        println("[Prune] $path 삭제")
        deleteRecursively()
    }
}
