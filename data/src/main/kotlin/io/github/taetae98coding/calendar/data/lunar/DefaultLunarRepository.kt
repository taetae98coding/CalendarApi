package io.github.taetae98coding.calendar.data.lunar

import io.github.taetae98coding.calendar.data.cache.CachePeriod
import io.github.taetae98coding.calendar.data.cache.CacheStore
import io.github.taetae98coding.calendar.data.cache.CacheUnit
import io.github.taetae98coding.calendar.data.cache.SourceApi
import io.github.taetae98coding.calendar.data.cache.SourceUpdater
import io.github.taetae98coding.calendar.data.cache.hasKasiItems
import io.github.taetae98coding.calendar.datasource.openapi.kasi.KasiDataSource
import io.github.taetae98coding.calendar.datasource.openapi.kasi.KasiLunarItem
import io.github.taetae98coding.calendar.domain.lunar.LunarDate
import io.github.taetae98coding.calendar.domain.lunar.LunarRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.serialization.json.JsonElement

/**
 * 음력 저장소.
 *
 * 출처가 한국천문연구원 음양력 정보 하나뿐이라 공휴일보다 단순하다.
 * @see io.github.taetae98coding.calendar.data.holiday.DefaultHolidayRepository
 */
data object DefaultLunarRepository : LunarRepository, SourceUpdater {
    override val subject: String = "음력"

    override val apis: List<SourceApi> = listOf(SourceApi.KASI_LUN_CAL)

    override suspend fun get(year: Int): List<LunarDate> {
        val items = coroutineScope {
            (1..12).map { month -> YearMonth(year, month) }
                .map { yearMonth -> async { items(yearMonth) } }
                .awaitAll()
                .flatten()
        }

        // 요청한 월과 응답의 양력 월이 어긋날 수 있으므로 응답에 적힌 실제 양력 날짜로 다시 거른다.
        return items.map(LunarMapper::toLunarDate)
            .filter { lunarDate -> lunarDate.solar.year == year }
            .sortedBy(LunarDate::solar)
            .distinctBy(LunarDate::solar)
    }

    override suspend fun fetch(unit: CacheUnit): Result<JsonElement?> = KasiDataSource.getLunar(unit.period.yearMonth)

    override fun hasItems(api: SourceApi, raw: JsonElement): Boolean = hasKasiItems(api, raw)

    private suspend fun items(yearMonth: YearMonth): List<KasiLunarItem> {
        val raw = CacheStore.read(SourceApi.KASI_LUN_CAL, CachePeriod(yearMonth.year, yearMonth.month.number)) ?: return emptyList()

        return runCatching { KasiDataSource.parseItems<KasiLunarItem>(raw, "getLunCalInfo $yearMonth") }
            .getOrElse { throwable ->
                println("[Data] getLunCalInfo $yearMonth 해석 실패: ${throwable.message}")

                emptyList()
            }
    }
}
