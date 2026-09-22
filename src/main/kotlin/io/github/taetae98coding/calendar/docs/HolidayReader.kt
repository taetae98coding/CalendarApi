package io.github.taetae98coding.calendar.docs

import io.github.taetae98coding.calendar.cache.CachePeriod
import io.github.taetae98coding.calendar.cache.CacheStore
import io.github.taetae98coding.calendar.cache.SourceApi
import io.github.taetae98coding.calendar.holiday.Country
import io.github.taetae98coding.calendar.holiday.Holiday
import io.github.taetae98coding.calendar.holiday.HolidayName
import io.github.taetae98coding.calendar.holiday.holidayDistinct
import io.github.taetae98coding.calendar.holiday.holidayFold
import io.github.taetae98coding.calendar.holiday.holidaySorted
import io.github.taetae98coding.calendar.openapi.kasi.KasiDataSource
import io.github.taetae98coding.calendar.openapi.kasi.KasiSpcdeItem
import io.github.taetae98coding.calendar.openapi.nager.NagerDataSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number

/** 캐시에 쌓인 원본만 읽어 한 해의 공휴일을 만든다. 네트워크를 타지 않는다. */
data object HolidayReader {
    suspend fun read(country: Country, year: Int): List<Holiday> {
        val holidays = when (country) {
            Country.KOREA -> korea(year)
            Country.UNITED_STATES -> nager(country, year)
        }

        return holidays.holidayDistinct()
            .holidayFold()
            .holidaySorted()
    }

    /**
     * 한국천문연구원 특일 정보를 우선 사용한다.
     * 특일 정보에 공휴일이 없는 연도(2004년 이전, 아직 고시되지 않은 미래 연도)는 Nager.Date 로 보완한다.
     */
    private suspend fun korea(year: Int): List<Holiday> {
        val items = coroutineScope {
            SourceApi.spcde.flatMap { api -> (1..12).map { month -> api to YearMonth(year, month) } }
                .map { (api, yearMonth) -> async { spcdeItems(api, yearMonth) } }
                .awaitAll()
                .flatten()
        }

        val kasiHolidays = items.map { item -> item.toHoliday() }
        if (kasiHolidays.any(Holiday::isHoliday)) return kasiHolidays

        val kasiDates = kasiHolidays.map(Holiday::start).toSet()

        return kasiHolidays + nager(Country.KOREA, year).filterNot { holiday -> holiday.start in kasiDates }
    }

    private suspend fun spcdeItems(api: SourceApi, yearMonth: YearMonth): List<KasiSpcdeItem> {
        val raw = CacheStore.read(api, CachePeriod(yearMonth.year, yearMonth.month.number)) ?: return emptyList()

        return runCatching { KasiDataSource.parseItems<KasiSpcdeItem>(raw, "${api.id} $yearMonth") }
            .getOrElse { throwable ->
                println("[Docs] ${api.id} $yearMonth 해석 실패: ${throwable.message}")

                emptyList()
            }
    }

    /** Nager.Date 는 연 단위 응답이라 캐시도 연 단위 파일 하나다. */
    private suspend fun nager(country: Country, year: Int): List<Holiday> {
        val api = SourceApi.nager(country)
        val raw = CacheStore.read(api, CachePeriod(year)) ?: return emptyList()

        val items = runCatching { NagerDataSource.parseHolidays(raw) }
            .getOrElse { throwable ->
                println("[Docs] ${api.id} $year 해석 실패: ${throwable.message}")

                return emptyList()
            }

        return items.filter { item -> item.counties == null }
            .map { item ->
                Holiday(
                    name = if (country == Country.KOREA) HolidayName.normalize(item.localName) else item.localName,
                    isHoliday = item.isPublicHoliday && item.isGlobal,
                    start = item.date,
                    endInclusive = item.date,
                )
            }
    }

    private fun KasiSpcdeItem.toHoliday(): Holiday {
        return Holiday(
            name = prettyName,
            isHoliday = isHoliday,
            start = date,
            endInclusive = date,
        )
    }
}
