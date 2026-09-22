package io.github.taetae98coding.calendar.domain

import io.github.taetae98coding.calendar.domain.holiday.Holiday
import io.github.taetae98coding.calendar.domain.holiday.holidayFold
import io.github.taetae98coding.calendar.domain.holiday.holidaySorted
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class HolidayFoldTest {
    private fun holiday(name: String, date: String) = LocalDate.parse(date).let { Holiday(name, true, it, it) }

    @Test
    fun `같은 이름의 연속된 날짜는 하나로 합쳐진다`() {
        val folded = listOf(
            holiday("설날", "2025-01-28"),
            holiday("설날", "2025-01-29"),
            holiday("설날", "2025-01-30"),
            holiday("신정", "2025-01-01"),
        ).holidayFold().holidaySorted()

        assertEquals(2, folded.size)
        assertEquals(LocalDate(2025, 1, 1), folded[0].start)
        assertEquals(LocalDate(2025, 1, 28), folded[1].start)
        assertEquals(LocalDate(2025, 1, 30), folded[1].endInclusive)
    }

    @Test
    fun `떨어진 날짜는 합쳐지지 않는다`() {
        val folded = listOf(
            holiday("현충일", "2025-06-06"),
            holiday("현충일", "2026-06-06"),
        ).holidayFold()

        assertEquals(2, folded.size)
    }
}
