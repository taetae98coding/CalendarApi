package io.github.taetae98coding.calendar.docs

import io.github.taetae98coding.calendar.cache.CachePeriod
import io.github.taetae98coding.calendar.cache.CacheStore
import io.github.taetae98coding.calendar.cache.SourceApi
import io.github.taetae98coding.calendar.lunar.LunarDate
import io.github.taetae98coding.calendar.openapi.kasi.KasiDataSource
import io.github.taetae98coding.calendar.openapi.kasi.KasiLunarItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number

/** 캐시에 쌓인 원본만 읽어 한 해의 음력을 만든다. 네트워크를 타지 않는다. */
data object LunarReader {
    suspend fun read(year: Int): List<LunarDate> {
        val items = coroutineScope {
            (1..12).map { month -> YearMonth(year, month) }
                .map { yearMonth -> async { items(yearMonth) } }
                .awaitAll()
                .flatten()
        }

        // 요청한 월과 응답의 양력 월이 어긋날 수 있으므로 응답에 적힌 실제 양력 날짜로 다시 거른다.
        return items.map { item -> item.toLunarDate() }
            .filter { lunarDate -> lunarDate.solar.year == year }
            .sortedBy(LunarDate::solar)
            .distinctBy(LunarDate::solar)
    }

    private suspend fun items(yearMonth: YearMonth): List<KasiLunarItem> {
        val raw = CacheStore.read(SourceApi.KASI_LUN_CAL, CachePeriod(yearMonth.year, yearMonth.month.number)) ?: return emptyList()

        return runCatching { KasiDataSource.parseItems<KasiLunarItem>(raw, "getLunCalInfo $yearMonth") }
            .getOrElse { throwable ->
                println("[Docs] getLunCalInfo $yearMonth 해석 실패: ${throwable.message}")

                emptyList()
            }
    }

    private fun KasiLunarItem.toLunarDate(): LunarDate {
        return LunarDate(
            solar = solarDate,
            year = lunarYearValue,
            month = lunarMonthValue,
            day = lunarDayValue,
            isLeapMonth = isLeapMonth,
        )
    }
}
