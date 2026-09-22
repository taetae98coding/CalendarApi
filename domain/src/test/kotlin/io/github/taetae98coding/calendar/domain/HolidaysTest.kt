package io.github.taetae98coding.calendar.domain

import io.github.taetae98coding.calendar.domain.holiday.Holiday
import io.github.taetae98coding.calendar.domain.holiday.distinctByDate
import io.github.taetae98coding.calendar.domain.holiday.foldConsecutive
import io.github.taetae98coding.calendar.domain.holiday.normalized
import io.github.taetae98coding.calendar.domain.holiday.sortedByStart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class HolidaysTest {
    private fun holiday(name: String, date: String, isHoliday: Boolean = true) = LocalDate.parse(date).let { Holiday(name, isHoliday, it, it) }

    @Test
    fun `같은 이름의 연속된 날짜는 하나로 합쳐진다`() {
        val folded = listOf(
            holiday("설날", "2025-01-28"),
            holiday("설날", "2025-01-29"),
            holiday("설날", "2025-01-30"),
            holiday("신정", "2025-01-01"),
        ).foldConsecutive().sortedByStart()

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
        ).foldConsecutive()

        assertEquals(2, folded.size)
    }

    @Test
    fun `같은 날짜에 isHoliday 가 엇갈리면 휴일로 합친다`() {
        val date = LocalDate(2025, 5, 5)

        // 특일 정보는 어린이날을 API 마다 Y/N 으로 다르게 내려준다. 도착 순서와 무관하게 같은 결과여야 한다.
        val ascending = listOf(
            Holiday("어린이날", false, date, date),
            Holiday("어린이날", true, date, date),
        ).distinctByDate()

        val descending = listOf(
            Holiday("어린이날", true, date, date),
            Holiday("어린이날", false, date, date),
        ).distinctByDate()

        assertEquals(1, ascending.size)
        assertEquals(ascending, descending)
        assertTrue(ascending.single().isHoliday)
    }

    @Test
    fun `정리는 중복 제거 - 기간 합치기 - 정렬 순서다`() {
        val normalized = listOf(
            holiday("설날", "2025-01-29"),
            holiday("설날", "2025-01-28", isHoliday = false),
            holiday("설날", "2025-01-28"),
            holiday("신정", "2025-01-01"),
        ).normalized()

        assertEquals(listOf("신정", "설날"), normalized.map(Holiday::name))
        assertEquals(LocalDate(2025, 1, 29), normalized[1].endInclusive)
        assertTrue(normalized[1].isHoliday)
    }

    @Test
    fun `같은 날 시작하면 이름순이다`() {
        val sorted = listOf(holiday("현충일", "2025-06-06"), holiday("망종", "2025-06-06")).sortedByStart()

        assertEquals(listOf("망종", "현충일"), sorted.map(Holiday::name))
    }
}
