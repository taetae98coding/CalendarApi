package io.github.taetae98coding.calendar.data.holiday

import io.github.taetae98coding.calendar.data.cache.CachePeriod
import io.github.taetae98coding.calendar.data.cache.CacheStore
import io.github.taetae98coding.calendar.data.cache.CacheUnit
import io.github.taetae98coding.calendar.data.cache.SourceApi
import io.github.taetae98coding.calendar.data.cache.SourceUpdater
import io.github.taetae98coding.calendar.data.cache.hasKasiItems
import io.github.taetae98coding.calendar.datasource.openapi.kasi.KasiDataSource
import io.github.taetae98coding.calendar.datasource.openapi.kasi.KasiSpcdeItem
import io.github.taetae98coding.calendar.datasource.openapi.nager.NagerDataSource
import io.github.taetae98coding.calendar.domain.holiday.Country
import io.github.taetae98coding.calendar.domain.holiday.Holiday
import io.github.taetae98coding.calendar.domain.holiday.HolidayRepository
import io.github.taetae98coding.calendar.domain.holiday.holidayDistinct
import io.github.taetae98coding.calendar.domain.holiday.holidayFold
import io.github.taetae98coding.calendar.domain.holiday.holidaySorted
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.serialization.json.JsonElement

/**
 * 공휴일 저장소.
 *
 * 읽기([get])는 캐시에 쌓인 원본만 본다. 네트워크를 타지 않는다.
 * 쓰기([fetch])는 갱신 단위 한 건을 받아 오는 것까지만 하고, 예산과 순서는 UpdateScheduler 가 정한다.
 *
 * 공휴일은 출처가 둘이다. 어느 쪽을 쓸지 아는 것이 이 클래스의 책임이고, 그 사실은 바깥으로 새지 않는다.
 */
data object DefaultHolidayRepository : HolidayRepository, SourceUpdater {
    override val subject: String = "공휴일"

    override val apis: List<SourceApi> = SourceApi.spcde + Country.entries.map(SourceApi::nager)

    override suspend fun get(country: Country, year: Int): List<Holiday> {
        val holidays = when (country) {
            Country.KOREA -> korea(year)
            Country.UNITED_STATES -> nager(country, year)
        }

        return holidays.holidayDistinct()
            .holidayFold()
            .holidaySorted()
    }

    override suspend fun fetch(unit: CacheUnit): Result<JsonElement?> {
        val country = unit.api.country

        return if (country == null) {
            KasiDataSource.getSpcde(unit.api.id, unit.period.yearMonth)
        } else {
            NagerDataSource.getHoliday(unit.period.year, country.nagerCode)
        }
    }

    override fun hasItems(api: SourceApi, raw: JsonElement): Boolean {
        return if (api.country == null) hasKasiItems(api, raw) else NagerDataSource.parseHolidays(raw).isNotEmpty()
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

        val kasiHolidays = items.map(KasiHolidayMapper::toHoliday)
        if (kasiHolidays.any(Holiday::isHoliday)) return kasiHolidays

        val kasiDates = kasiHolidays.map(Holiday::start).toSet()

        return kasiHolidays + nager(Country.KOREA, year).filterNot { holiday -> holiday.start in kasiDates }
    }

    private suspend fun spcdeItems(api: SourceApi, yearMonth: YearMonth): List<KasiSpcdeItem> {
        val raw = CacheStore.read(api, CachePeriod(yearMonth.year, yearMonth.month.number)) ?: return emptyList()

        return runCatching { KasiDataSource.parseItems<KasiSpcdeItem>(raw, "${api.id} $yearMonth") }
            .getOrElse { throwable ->
                println("[Data] ${api.id} $yearMonth 해석 실패: ${throwable.message}")

                emptyList()
            }
    }

    /** Nager.Date 는 연 단위 응답이라 캐시도 연 단위 파일 하나다. */
    private suspend fun nager(country: Country, year: Int): List<Holiday> {
        val api = SourceApi.nager(country)
        val raw = CacheStore.read(api, CachePeriod(year)) ?: return emptyList()

        val items = runCatching { NagerDataSource.parseHolidays(raw) }
            .getOrElse { throwable ->
                println("[Data] ${api.id} $year 해석 실패: ${throwable.message}")

                return emptyList()
            }

        return NagerHolidayMapper.toHolidays(items, country)
    }
}
