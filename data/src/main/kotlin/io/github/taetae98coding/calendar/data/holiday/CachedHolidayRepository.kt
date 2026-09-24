package io.github.taetae98coding.calendar.data.holiday

import io.github.taetae98coding.calendar.data.cache.CachePeriod
import io.github.taetae98coding.calendar.data.cache.CacheReader
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

/**
 * 공휴일 저장소. 캐시에 쌓인 원본만 읽는다. 네트워크를 타지 않는다.
 *
 * 공휴일은 출처가 둘이다. 어느 쪽을 쓸지 아는 것이 이 클래스의 책임이고, 그 사실은 바깥으로 새지 않는다.
 * 캐시가 없거나 깨진 구간은 [CacheReader] 가 비어 있는 것으로 돌려준다.
 */
class CachedHolidayRepository(
    private val reader: CacheReader,
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
        // 어느 API 의 항목인지 알아야 API 별 후처리를 할 수 있으므로, 합치기 전에 API 단위로 매핑한다.
        val kasiHolidays = coroutineScope {
            SourceApi.spcde.flatMap { api -> (1..12).map { month -> api to YearMonth(year, month) } }
                .map { (api, yearMonth) -> async { HolidayMapper.fromKasi(api, spcdeItems(api, yearMonth)) } }
                .awaitAll()
                .flatten()
        }

        if (kasiHolidays.any(Holiday::isHoliday)) return kasiHolidays

        val kasiDates = kasiHolidays.map(Holiday::start).toSet()

        return kasiHolidays + nager(Country.KOREA, year).filterNot { holiday -> holiday.start in kasiDates }
    }

    private suspend fun spcdeItems(api: SourceApi, yearMonth: YearMonth): List<KasiSpcdeItem> {
        return reader.items(api, CachePeriod.of(yearMonth)) { raw -> KasiResponse.items(raw, "${api.id} $yearMonth") }
    }

    /** Nager.Date 는 연 단위 응답이라 캐시도 연 단위 파일 하나다. */
    private suspend fun nager(country: Country, year: Int): List<Holiday> {
        val api = SourceApi.nager(country)
        val items = reader.items(api, CachePeriod(year), NagerResponse::holidays)

        return HolidayMapper.fromNager(api, items)
    }
}
