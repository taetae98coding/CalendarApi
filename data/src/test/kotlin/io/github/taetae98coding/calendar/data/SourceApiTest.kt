package io.github.taetae98coding.calendar.data

import io.github.taetae98coding.calendar.data.source.CacheProvider
import io.github.taetae98coding.calendar.data.source.FetchService
import io.github.taetae98coding.calendar.data.source.Remote
import io.github.taetae98coding.calendar.data.source.SourceApi
import io.github.taetae98coding.calendar.domain.Country
import kotlin.test.Test
import kotlin.test.assertEquals

/** 원천 식별자가 곧 캐시 폴더 이름이다. 바뀌면 받아 둔 자료를 잃는다. */
class SourceApiTest {
    @Test
    fun `캐시 폴더 이름은 원천의 표기를 따른다`() {
        assertEquals("getRestDeInfo", SourceApi.KASI_REST_DE.id)
        assertEquals("publicHolidays-kr", SourceApi.NAGER_KOREA.id)
        assertEquals("publicHolidays-us", SourceApi.NAGER_UNITED_STATES.id)
    }

    @Test
    fun `중단 단위는 공공데이터포털 서비스와 같다`() {
        assertEquals(setOf(FetchService.KASI_SPCDE), SourceApi.spcde.map(SourceApi::service).toSet())
        assertEquals(FetchService.KASI_LUNAR, SourceApi.KASI_LUN_CAL.service)
        assertEquals(FetchService.NAGER, SourceApi.NAGER_KOREA.service)
    }

    @Test
    fun `Nager 는 국가 코드를 대문자로 보낸다`() {
        val remote = SourceApi.nager(Country.UNITED_STATES).remote as Remote.Nager

        assertEquals("US", remote.countryCode)
        assertEquals(2, SourceApi.of(CacheProvider.NAGER).size)
        assertEquals(6, SourceApi.of(CacheProvider.KASI).size)
    }
}
