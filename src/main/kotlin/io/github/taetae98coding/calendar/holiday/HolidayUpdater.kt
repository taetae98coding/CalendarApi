package io.github.taetae98coding.calendar.holiday

import io.github.taetae98coding.calendar.Config
import io.github.taetae98coding.calendar.Paths
import io.github.taetae98coding.calendar.file.FileDataSource
import io.github.taetae98coding.calendar.openapi.kasi.KasiDataSource
import io.github.taetae98coding.calendar.openapi.kasi.KasiSpcdeItem
import io.github.taetae98coding.calendar.openapi.nager.NagerDataSource
import io.github.taetae98coding.calendar.openapi.nager.NagerHoliday
import java.io.File
import kotlin.time.Clock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.todayIn
import kotlinx.datetime.yearMonth

data object HolidayUpdater {
    private val spcdeApis = listOf(
        "getRestDeInfo",
        "getHoliDeInfo",
        "getAnniversaryInfo",
        "get24DivisionsInfo",
        "getSundryDayInfo",
    )

    /** 국가 -> 연도 -> 공휴일 목록. 통합 API 생성에 재사용한다. */
    suspend fun update(config: Config, isSpcdeRegistered: Boolean): Map<Country, Map<Int, List<Holiday>>> {
        return coroutineScope {
            Country.entries
                .map { country ->
                    async {
                        val byYear = config.years
                            .map { year -> async { year to updateYear(country, year, config, isSpcdeRegistered) } }
                            .awaitAll()
                            .toMap()

                        country to byYear
                    }
                }
                .awaitAll()
                .toMap()
        }
    }

    private suspend fun updateYear(country: Country, year: Int, config: Config, isSpcdeRegistered: Boolean): List<Holiday> {
        val holidays = when (country) {
            Country.KOREA -> koreaHolidays(year, config, isSpcdeRegistered)
            Country.UNITED_STATES -> nagerHolidays(country, year, config)
        }
            .holidayDistinct()
            .holidayFold()
            .holidaySorted()

        coroutineScope {
            launch { writeSafely(holidays, Paths.holidayYear(country, year)) }

            (1..12).map { month -> YearMonth(year, month) }
                .forEach { yearMonth ->
                    launch { writeSafely(holidays.filter { holiday -> holiday.overlaps(yearMonth) }, Paths.holidayYearMonth(country, yearMonth)) }
                }
        }

        return holidays
    }

    /**
     * 한국천문연구원 특일 정보를 우선 사용한다.
     * 특일 정보에 공휴일이 없는 연도(2004년 이전, 아직 고시되지 않은 미래 연도)는 Nager.Date 로 보완한다.
     */
    private suspend fun koreaHolidays(year: Int, config: Config, isSpcdeRegistered: Boolean): List<Holiday> {
        if (!isSpcdeRegistered) return nagerHolidays(Country.KOREA, year, config)

        val items = coroutineScope {
            (1..12).map { month -> YearMonth(year, month) }
                .flatMap { yearMonth -> spcdeApis.map { api -> yearMonth to api } }
                .map { (yearMonth, api) -> async { spcdeItems(api, yearMonth, config) } }
                .awaitAll()
                .flatten()
        }

        val kasiHolidays = items.map { item -> item.toHoliday() }
        if (kasiHolidays.any(Holiday::isHoliday)) return kasiHolidays

        val fallback = nagerHolidays(Country.KOREA, year, config)
        if (fallback.isNotEmpty()) {
            println("[Holiday] KR $year : 특일 정보에 공휴일이 없어 Nager.Date 로 보완합니다.")
        }

        val kasiDates = kasiHolidays.map(Holiday::start).toSet()

        return kasiHolidays + fallback.filterNot { it.start in kasiDates }
    }

    private suspend fun spcdeItems(api: String, yearMonth: YearMonth, config: Config): List<KasiSpcdeItem> {
        val file = Paths.kasiSpcdeCache(api, yearMonth)

        if (canUseCache(yearMonth, config)) {
            FileDataSource.readOrNull<List<KasiSpcdeItem>>(file)?.let { return it }
        }

        return runCatching { KasiDataSource.getSpcdeItems(api, yearMonth) }
            .onSuccess { items -> FileDataSource.write(items, file) }
            .getOrElse { throwable ->
                println("[Holiday] KASI $api $yearMonth 조회 실패: ${throwable.message}")
                FileDataSource.readOrNull<List<KasiSpcdeItem>>(file).orEmpty()
            }
    }

    private suspend fun nagerHolidays(country: Country, year: Int, config: Config): List<Holiday> {
        val file = Paths.nagerCache(country, year)
        val today = Clock.System.todayIn(TimeZone.of("Asia/Seoul"))

        val items = if (!config.fetchEnforce && year < today.year) {
            FileDataSource.readOrNull<List<NagerHoliday>>(file)
                ?: fetchNager(country, year, file)
        } else {
            fetchNager(country, year, file)
        }

        return items.filter { it.counties == null }
            .map { item ->
                Holiday(
                    name = if (country == Country.KOREA) HolidayName.normalize(item.localName) else item.localName,
                    isHoliday = item.isPublicHoliday && item.isGlobal,
                    start = item.date,
                    endInclusive = item.date,
                )
            }
    }

    private suspend fun fetchNager(country: Country, year: Int, file: File): List<NagerHoliday> {
        return runCatching { NagerDataSource.getHoliday(year, country.nagerCode) }
            .onSuccess { items -> if (items.isNotEmpty()) FileDataSource.write(items, file) }
            .getOrElse { throwable ->
                println("[Holiday] Nager.Date ${country.nagerCode} $year 조회 실패: ${throwable.message}")
                FileDataSource.readOrNull<List<NagerHoliday>>(file).orEmpty()
            }
    }

    private fun canUseCache(yearMonth: YearMonth, config: Config): Boolean {
        if (config.fetchEnforce) return false

        val today = Clock.System.todayIn(TimeZone.of("Asia/Seoul"))

        return yearMonth < today.yearMonth
    }

    /** 일시적인 조회 실패로 이미 배포된 데이터가 빈 배열로 덮어써지는 것을 막는다. */
    private suspend fun writeSafely(holidays: List<Holiday>, file: File) {
        if (holidays.isEmpty() && FileDataSource.readOrNull<List<Holiday>>(file)?.isNotEmpty() == true) {
            println("[Holiday] ${file.path} : 조회 결과가 비어 있어 기존 파일을 유지합니다.")
            return
        }

        FileDataSource.write(holidays, file)
    }

    private fun Holiday.overlaps(yearMonth: YearMonth): Boolean {
        return start.yearMonth <= yearMonth && yearMonth <= endInclusive.yearMonth
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
