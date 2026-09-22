package io.github.taetae98coding.calendar.publish

import io.github.taetae98coding.calendar.domain.Country
import java.io.File
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number

/**
 * 배포 경로 규칙. `docs/{api}/{country}/{year}.json` 과 `docs/{api}/{country}/{year}/{month}.json`
 *
 * 이 경로가 곧 공개 URL 이라 한 번 정하면 바꾸기 어렵다. 캐시 경로와 분리해 둔다.
 */
class DocPaths(
    val root: File,
) {
    val meta: File = File(root, META_FILE_NAME)

    val index: File = File(root, INDEX_FILE_NAME)

    /** GitHub Pages 가 밑줄로 시작하는 경로를 Jekyll 규칙으로 걸러내지 않게 한다. */
    val noJekyll: File = File(root, NO_JEKYLL_FILE_NAME)

    fun api(api: DocApi): File = File(root, api.id)

    fun country(api: DocApi, country: Country): File = File(api(api), country.code)

    fun year(api: DocApi, country: Country, year: Int): File = File(country(api, country), "$year.json")

    fun month(api: DocApi, country: Country, yearMonth: YearMonth): File {
        return File(country(api, country), "${yearMonth.year}/${yearMonth.month.number.toString().padStart(2, '0')}.json")
    }

    companion object {
        const val META_FILE_NAME = "meta.json"
        const val INDEX_FILE_NAME = "index.html"
        const val NO_JEKYLL_FILE_NAME = ".nojekyll"

        /** docs 루트에서 API 폴더가 아니어도 남겨야 하는 파일. */
        val rootFiles: Set<String> = setOf(META_FILE_NAME, INDEX_FILE_NAME, NO_JEKYLL_FILE_NAME)
    }
}
