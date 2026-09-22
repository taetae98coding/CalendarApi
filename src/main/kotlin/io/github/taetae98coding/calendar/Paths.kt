package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.cache.CachePeriod
import io.github.taetae98coding.calendar.cache.SourceApi
import io.github.taetae98coding.calendar.docs.DocApi
import io.github.taetae98coding.calendar.holiday.Country
import java.io.File
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number

/**
 * 파일 경로 규칙.
 *
 * - 캐시 : `cache/{provider}/{api}/{year}/{month}.json` (연 단위 응답은 `cache/{provider}/{api}/{year}.json`)
 * - 배포 : `docs/{api}/{country}/{year}.json` 과 `docs/{api}/{country}/{year}/{month}.json`
 */
data object Paths {
    val docs = File("docs")
    val cache = File("cache")

    fun cacheProvider(api: SourceApi): File = File(cache, api.provider.id)

    fun cacheApi(api: SourceApi): File = File(cacheProvider(api), api.id)

    /** 구간 하나의 응답 원본. 월 단위 API 는 `{year}/{month}.json`, 연 단위 API 는 `{year}.json`. */
    fun cacheFile(api: SourceApi, period: CachePeriod): File = File(cacheApi(api), "${period.key}.json")

    /** API 하나의 구간별 마지막 갱신 시각. 월 파일·연 파일과 이름이 겹치지 않는다. */
    fun cacheMeta(api: SourceApi): File = File(cacheApi(api), "meta.json")

    fun docApi(api: DocApi): File = File(docs, api.id)

    fun docCountry(api: DocApi, country: Country): File = File(docApi(api), country.code)

    fun docYear(api: DocApi, country: Country, year: Int): File = File(docCountry(api, country), "$year.json")

    fun docMonth(api: DocApi, country: Country, yearMonth: YearMonth): File {
        return File(docCountry(api, country), "${yearMonth.year}/${yearMonth.pad()}.json")
    }

    val meta = File(docs, "meta.json")

    private fun YearMonth.pad(): String = month.number.toString().padStart(2, '0')
}
