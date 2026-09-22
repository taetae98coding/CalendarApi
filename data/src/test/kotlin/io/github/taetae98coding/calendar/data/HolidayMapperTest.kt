package io.github.taetae98coding.calendar.data

import io.github.taetae98coding.calendar.data.holiday.KasiHolidayMapper
import io.github.taetae98coding.calendar.data.holiday.NagerHolidayMapper
import io.github.taetae98coding.calendar.datasource.kasi.KasiSpcdeItem
import io.github.taetae98coding.calendar.datasource.kasi.KasiSpcdeItemKind
import io.github.taetae98coding.calendar.datasource.nager.NagerHoliday
import io.github.taetae98coding.calendar.domain.Country
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/**
 * 이름을 맞추는 일은 원천 DTO 가 아니라 매퍼가 한다.
 * 출처가 늘어도 도메인은 그대로고 매퍼만 추가된다.
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
        assertEquals("신정", KasiHolidayMapper.toHoliday(kasi("1월1일")).name)
        assertEquals("신정", NagerHolidayMapper.toHolidays(listOf(nager("새해")), Country.KOREA).single().name)
    }

    @Test
    fun `한국이 아니면 현지 표기를 그대로 쓴다`() {
        val holiday = NagerHolidayMapper.toHolidays(listOf(nager("New Year's Day")), Country.UNITED_STATES).single()

        assertEquals("New Year's Day", holiday.name)
    }

    @Test
    fun `지역 한정 공휴일은 내보내지 않는다`() {
        val holidays = NagerHolidayMapper.toHolidays(
            items = listOf(nager("Good Friday", counties = listOf("US-CT")), nager("New Year's Day")),
            country = Country.UNITED_STATES,
        )

        assertEquals(listOf("New Year's Day"), holidays.map { holiday -> holiday.name })
    }

    @Test
    fun `공휴일 여부는 출처의 판정을 따른다`() {
        assertTrue(KasiHolidayMapper.toHoliday(kasi("어린이날")).isHoliday)

        val observance = NagerHoliday(date = date, localName = "Valentine's Day", name = "Valentine's Day", types = listOf("Observance"))

        assertFalse(NagerHolidayMapper.toHolidays(listOf(observance), Country.UNITED_STATES).single().isHoliday)
    }
}
