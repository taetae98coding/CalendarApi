package io.github.taetae98coding.calendar.data.lunar

import io.github.taetae98coding.calendar.data.Logger
import io.github.taetae98coding.calendar.data.cache.CachePeriod
import io.github.taetae98coding.calendar.data.cache.CacheStore
import io.github.taetae98coding.calendar.data.source.SourceApi
import io.github.taetae98coding.calendar.datasource.kasi.KasiLunarItem
import io.github.taetae98coding.calendar.datasource.kasi.KasiResponse
import io.github.taetae98coding.calendar.domain.lunar.LunarDate
import io.github.taetae98coding.calendar.domain.lunar.LunarRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.YearMonth

/**
 * 음력 저장소. 캐시에 쌓인 원본만 읽는다.
 *
 * 출처가 한국천문연구원 음양력 정보 하나뿐이라 공휴일보다 단순하다.
 * @see io.github.taetae98coding.calendar.data.holiday.DefaultHolidayRepository
 */
class DefaultLunarRepository(
    private val store: CacheStore,
    private val logger: Logger,
) : LunarRepository {
    override suspend fun get(year: Int): List<LunarDate> {
        val items = coroutineScope {
            (1..12).map { month -> async { items(YearMonth(year, month)) } }
                .awaitAll()
                .flatten()
        }

        // 요청한 월과 응답의 양력 월이 어긋날 수 있으므로 응답에 적힌 실제 양력 날짜로 다시 거른다.
        return items.map(LunarMapper::toLunarDate)
            .filter { lunarDate -> lunarDate.solar.year == year }
            .sortedBy(LunarDate::solar)
            .distinctBy(LunarDate::solar)
    }

    private suspend fun items(yearMonth: YearMonth): List<KasiLunarItem> {
        val api = SourceApi.KASI_LUN_CAL

        return runCatching { store.read(api, CachePeriod.of(yearMonth))?.let { raw -> KasiResponse.items<KasiLunarItem>(raw, "${api.id} $yearMonth") }.orEmpty() }
            .getOrElse { throwable ->
                logger.log("[Data] ${api.id} $yearMonth 해석 실패: ${throwable.message}")

                emptyList()
            }
    }
}
