package io.github.taetae98coding.calendar.publish

import io.github.taetae98coding.calendar.domain.holiday.Country
import java.io.File
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number

/**
 * 배포 경로 규칙. `docs/{api}/{country}/{year}.json` 과 `docs/{api}/{country}/{year}/{month}.json`
 *
 * 이 경로가 곧 공개 URL 이라 한 번 정하면 바꾸기 어렵다. 캐시 경로와 분리해 둔다.
 */
data object DocPaths {
    val root = File("docs")

    val meta = File(root, "meta.json")

    fun api(api: DocApi): File = File(root, api.id)

    fun country(api: DocApi, country: Country): File = File(api(api), country.code)

    fun year(api: DocApi, country: Country, year: Int): File = File(country(api, country), "$year.json")

    fun month(api: DocApi, country: Country, yearMonth: YearMonth): File {
        return File(country(api, country), "${yearMonth.year}/${yearMonth.pad()}.json")
    }

    private fun YearMonth.pad(): String = month.number.toString().padStart(2, '0')
}
