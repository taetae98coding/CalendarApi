package io.github.taetae98coding.calendar.data

import io.github.taetae98coding.calendar.data.cache.CachePaths
import io.github.taetae98coding.calendar.data.cache.CachePeriod
import io.github.taetae98coding.calendar.data.cache.CacheReader
import io.github.taetae98coding.calendar.data.cache.CacheStore
import io.github.taetae98coding.calendar.data.holiday.CachedHolidayRepository
import io.github.taetae98coding.calendar.data.source.SourceApi
import io.github.taetae98coding.calendar.domain.Country
import io.github.taetae98coding.calendar.domain.holiday.Holiday
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class CachedHolidayRepositoryTest {
    private val root = tempDirectory()
    private val paths = CachePaths(root)
    private val store = CacheStore(paths)
    private val logger = RecordingLogger()
    private val repository = CachedHolidayRepository(CacheReader(store, logger))

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    private suspend fun cache(api: SourceApi, period: CachePeriod, body: String) = store.write(api, period, Json.parseToJsonElement(body))

    @Test
    fun `특일 정보의 여러 API 를 합쳐 정리한다`() = runTest {
        cache(SourceApi.KASI_REST_DE, CachePeriod(2025, 1), Fixtures.spcde(Fixtures.spcdeItem("1월1일", 20250101, true), Fixtures.spcdeItem("설날", 20250128, true), Fixtures.spcdeItem("설날", 20250129, true)))
        cache(SourceApi.KASI_HOLI_DE, CachePeriod(2025, 1), Fixtures.spcde(Fixtures.spcdeItem("설날", 20250130, true)))
        cache(SourceApi.KASI_ANNIVERSARY, CachePeriod(2025, 5), Fixtures.spcde(Fixtures.spcdeItem("어린이날", 20250505, false, kind = "02")))
        cache(SourceApi.KASI_REST_DE, CachePeriod(2025, 5), Fixtures.spcde(Fixtures.spcdeItem("어린이날", 20250505, true)))
        cache(SourceApi.NAGER_KOREA, CachePeriod(2025), Fixtures.nager(Fixtures.nagerItem("2025-12-25", "크리스마스")))

        val holidays = repository.get(Country.KOREA, 2025)

        assertEquals(listOf("신정", "설날", "어린이날"), holidays.map(Holiday::name))
        assertEquals(LocalDate(2025, 1, 30), holidays[1].endInclusive)
        assertTrue(holidays[2].isHoliday, "API 마다 다른 판정은 휴일로 합친다")
    }

    @Test
    fun `특일 정보에 공휴일이 없는 연도는 Nager 로 보완한다`() = runTest {
        // 24절기만 있고 공휴일이 없는 연도. Nager 의 같은 날짜는 KASI 쪽이 이긴다.
        cache(SourceApi.KASI_24_DIVISIONS, CachePeriod(2003, 1), Fixtures.spcde(Fixtures.spcdeItem("소한", 20030106, false, kind = "03")))
        cache(SourceApi.NAGER_KOREA, CachePeriod(2003), Fixtures.nager(Fixtures.nagerItem("2003-01-01", "새해"), Fixtures.nagerItem("2003-01-06", "소한(중복)")))

        val holidays = repository.get(Country.KOREA, 2003)

        assertEquals(listOf("신정", "소한"), holidays.map(Holiday::name))
    }

    @Test
    fun `미국은 Nager 만 쓰고 이름을 그대로 둔다`() = runTest {
        cache(SourceApi.NAGER_UNITED_STATES, CachePeriod(2026), Fixtures.nager(Fixtures.nagerItem("2026-01-01", "New Year's Day"), Fixtures.nagerItem("2026-04-03", "Good Friday", counties = "[\"US-CT\"]")))

        val holidays = repository.get(Country.UNITED_STATES, 2026)

        assertEquals(listOf("New Year's Day"), holidays.map(Holiday::name))
    }

    @Test
    fun `캐시가 없거나 깨진 구간은 비어 있는 것으로 보고 알린다`() = runTest {
        File(paths.file(SourceApi.NAGER_UNITED_STATES, CachePeriod(2026)).apply { parentFile.mkdirs() }.path).writeText("{not json")

        assertTrue(repository.get(Country.UNITED_STATES, 2026).isEmpty())
        assertTrue(repository.get(Country.UNITED_STATES, 2027).isEmpty())
        assertEquals(1, logger.messages.count { message -> "해석 실패" in message })
    }
}
