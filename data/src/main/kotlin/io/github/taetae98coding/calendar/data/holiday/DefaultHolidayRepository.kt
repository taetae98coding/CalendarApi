package io.github.taetae98coding.calendar.data.holiday

import io.github.taetae98coding.calendar.data.Logger
import io.github.taetae98coding.calendar.data.cache.CachePeriod
import io.github.taetae98coding.calendar.data.cache.CacheStore
import io.github.taetae98coding.calendar.data.source.SourceApi
import io.github.taetae98coding.calendar.datasource.kasi.KasiResponse
import io.github.taetae98coding.calendar.datasource.kasi.KasiSpcdeItem
import io.github.taetae98coding.calendar.datasource.nager.NagerResponse
import io.github.taetae98coding.calendar.domain.Country
import io.github.taetae98coding.calendar.domain.holiday.Holiday
import io.github.taetae98coding.calendar.domain.holiday.HolidayRepository
import io.github.taetae98coding.calendar.domain.holiday.normalized
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.YearMonth
import kotlinx.serialization.json.JsonElement

/**
 * 공휴일 저장소. 캐시에 쌓인 원본만 읽는다. 네트워크를 타지 않는다.
 *
 * 공휴일은 출처가 둘이다. 어느 쪽을 쓸지 아는 것이 이 클래스의 책임이고, 그 사실은 바깥으로 새지 않는다.
 * 캐시가 없거나 깨진 구간은 비어 있는 것으로 보고 로그만 남긴다. 다음 갱신이 다시 채운다.
 */
class DefaultHolidayRepository(
    private val store: CacheStore,
    private val logger: Logger,
) : HolidayRepository {
    override suspend fun get(country: Country, year: Int): List<Holiday> {
        val holidays = when (country) {
            Country.KOREA -> korea(year)
            Country.UNITED_STATES -> nager(country, year)
        }

        return holidays.normalized()
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
        return read(api, CachePeriod.of(yearMonth)) { raw -> KasiResponse.items(raw, "${api.id} $yearMonth") }
    }

    /** Nager.Date 는 연 단위 응답이라 캐시도 연 단위 파일 하나다. */
    private suspend fun nager(country: Country, year: Int): List<Holiday> {
        val items = read(SourceApi.nager(country), CachePeriod(year), NagerResponse::holidays)

        return NagerHolidayMapper.toHolidays(items, country)
    }

    private suspend fun <T> read(api: SourceApi, period: CachePeriod, parse: (raw: JsonElement) -> List<T>): List<T> {
        return runCatching { store.read(api, period)?.let(parse).orEmpty() }
            .getOrElse { throwable ->
                logger.log("[Data] ${api.id} ${period.key} 해석 실패: ${throwable.message}")

                emptyList()
            }
    }
}
