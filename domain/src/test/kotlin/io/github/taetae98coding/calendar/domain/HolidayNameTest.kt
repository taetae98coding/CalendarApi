package io.github.taetae98coding.calendar.domain

import io.github.taetae98coding.calendar.domain.holiday.HolidayName
import kotlin.test.Test
import kotlin.test.assertEquals

class HolidayNameTest {
    @Test
    fun `출처가 달라도 같은 이름으로 맞춘다`() {
        assertEquals("신정", HolidayName.normalize("새해"))
        assertEquals("신정", HolidayName.normalize("1월1일"))
        assertEquals("삼일절", HolidayName.normalize("3·1절"))
        assertEquals("부처님오신날", HolidayName.normalize("석가탄신일"))
        assertEquals("부처님오신날", HolidayName.normalize("부처님 오신 날"))
        assertEquals("크리스마스", HolidayName.normalize("기독탄신일"))
        assertEquals("어린이날", HolidayName.normalize("어린이 날"))
        assertEquals("대체공휴일", HolidayName.normalize("대체휴무일"))
    }

    @Test
    fun `선거 이름에서 대수를 뗀다`() {
        assertEquals("대통령선거", HolidayName.normalize("제21대 대통령 선거"))
        assertEquals("국회의원선거", HolidayName.normalize("제21대 국회의원선거"))
        assertEquals("국회의원선거", HolidayName.normalize("국회의원선거일"))
        assertEquals("전국동시지방선거", HolidayName.normalize("동시지방선거일"))
    }

    @Test
    fun `임시공휴일 괄호 안의 대상을 이름으로 쓴다`() {
        assertEquals("국군의 날", HolidayName.normalize("임시공휴일(국군의 날)"))
        assertEquals("임시공휴일", HolidayName.normalize("임시공휴일"))
    }

    @Test
    fun `알 수 없는 이름은 공백만 정리해 그대로 둔다`() {
        assertEquals("정월대보름", HolidayName.normalize("정월대보름"))
        assertEquals("6·25 전쟁일", HolidayName.normalize("  6·25  전쟁일 "))
    }
}
