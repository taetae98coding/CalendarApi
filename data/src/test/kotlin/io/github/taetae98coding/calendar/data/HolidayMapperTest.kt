package io.github.taetae98coding.calendar.data

import io.github.taetae98coding.calendar.data.holiday.HolidayMapper
import io.github.taetae98coding.calendar.data.source.SourceApi
import io.github.taetae98coding.calendar.datasource.kasi.KasiSpcdeItem
import io.github.taetae98coding.calendar.datasource.kasi.KasiSpcdeItemKind
import io.github.taetae98coding.calendar.datasource.nager.NagerHoliday
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/**
 * 이름을 맞추는 일은 원천 DTO 가 아니라 매퍼가 한다.
 * 공통 처리는 모든 API 에 적용되므로 API 가 늘어도 따로 챙길 것이 없다.
 */
class HolidayMapperTest {
    private val date = LocalDate(2025, 1, 1)

    private fun kasi(name: String) = KasiSpcdeItem(KasiSpcdeItemKind.NationalHoliday, name, true, date)

    private fun nager(localName: String, counties: List<String>? = null) = NagerHoliday(
        date = date,
        localName = localName,
        name = localName,
        types = listOf("Public"),
        counties = counties,
    )

    @Test
    fun `두 출처의 다른 표기가 같은 이름이 된다`() {
        assertEquals("신정", HolidayMapper.fromKasi(SourceApi.KASI_REST_DE, listOf(kasi("1월1일"))).single().name)
        assertEquals("신정", HolidayMapper.fromNager(SourceApi.NAGER_KOREA, listOf(nager("새해"))).single().name)
    }

    @Test
    fun `공통 처리는 특일 정보의 모든 API 에 적용된다`() {
        SourceApi.spcde.forEach { api ->
            assertEquals("신정", HolidayMapper.fromKasi(api, listOf(kasi("1월1일"))).single().name, api.id)
        }
    }

    @Test
    fun `한국어가 아닌 표기는 공통 처리를 거쳐도 그대로다`() {
        val holiday = HolidayMapper.fromNager(SourceApi.NAGER_UNITED_STATES, listOf(nager("New Year's Day"))).single()

        assertEquals("New Year's Day", holiday.name)
    }

    @Test
    fun `지역 한정 공휴일은 내보내지 않는다`() {
        val holidays = HolidayMapper.fromNager(
            api = SourceApi.NAGER_UNITED_STATES,
            items = listOf(nager("Good Friday", counties = listOf("US-CT")), nager("New Year's Day")),
        )

        assertEquals(listOf("New Year's Day"), holidays.map { holiday -> holiday.name })
    }

    @Test
    fun `공휴일 여부는 출처의 판정을 따른다`() {
        assertTrue(HolidayMapper.fromKasi(SourceApi.KASI_REST_DE, listOf(kasi("어린이날"))).single().isHoliday)

        val observance = NagerHoliday(date = date, localName = "Valentine's Day", name = "Valentine's Day", types = listOf("Observance"))

        assertFalse(HolidayMapper.fromNager(SourceApi.NAGER_UNITED_STATES, listOf(observance)).single().isHoliday)
    }
}
