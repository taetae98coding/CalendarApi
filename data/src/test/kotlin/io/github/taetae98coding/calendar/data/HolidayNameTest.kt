package io.github.taetae98coding.calendar.data

import io.github.taetae98coding.calendar.data.holiday.HolidayName
import kotlin.test.Test
import kotlin.test.assertEquals

class HolidayNameTest {
    @Test
    fun `출처가 달라도 같은 이름으로 맞춘다`() {
        assertEquals("신정", HolidayName.normalize("새해"))
        assertEquals("신정", HolidayName.normalize("1월1일"))
        assertEquals("삼일절", HolidayName.normalize("3·1절"))
        assertEquals("크리스마스", HolidayName.normalize("기독탄신일"))
        assertEquals("어버이날", HolidayName.normalize("어버이의 날"))
    }

    @Test
    fun `띄어쓰기만 다른 표기는 맞춤법에 맞는 한 가지로 모은다`() {
        listOf("어린이날", "어린이 날").forEach { raw -> assertEquals("어린이날", HolidayName.normalize(raw)) }
        listOf("4·19 혁명 기념일", "4·19혁명 기념일").forEach { raw -> assertEquals("4·19 혁명 기념일", HolidayName.normalize(raw)) }
        listOf("5·18 민주화운동기념일", "5·18 민주화운동 기념일", "5·18민주화운동 기념일", "5·18 민주화 운동 기념일").forEach { raw ->
            assertEquals("5·18 민주화 운동 기념일", HolidayName.normalize(raw))
        }
        listOf("환경의날", "환경의 날").forEach { raw -> assertEquals("환경의 날", HolidayName.normalize(raw)) }
    }

    @Test
    fun `법령이 명칭을 바꾸면 예전 연도도 바뀐 명칭을 법령 표기대로 쓴다`() {
        listOf("석가탄신일", "부처님오신날", "부처님 오신 날").forEach { raw -> assertEquals("부처님오신날", HolidayName.normalize(raw)) }
        listOf("근로자의 날", "노동절").forEach { raw -> assertEquals("노동절", HolidayName.normalize(raw)) }
        listOf("학생의 날", "학생독립운동기념일", "학생독립운동 기념일").forEach { raw -> assertEquals("학생독립운동 기념일", HolidayName.normalize(raw)) }
        listOf("임시정부수립기념일", "임시정부수립 기념일", "대한민국임시정부 수립기념일").forEach { raw ->
            assertEquals("대한민국 임시정부 수립 기념일", HolidayName.normalize(raw))
        }
    }

    @Test
    fun `임시공휴일과 대체공휴일은 출처의 표기를 그대로 쓴다`() {
        listOf("임시공휴일", "임시공휴일(제21대 대통령 선거)", "대체공휴일", "대체공휴일(설날)", "대체휴무일").forEach { raw ->
            assertEquals(raw, HolidayName.normalize(raw))
        }
    }

    @Test
    fun `명절은 명절 이름 하나로 모은다`() {
        listOf("설날", "설", "구정", "설날 연휴", "설날 전날", "설날 다음날").forEach { raw -> assertEquals("설날", HolidayName.normalize(raw)) }
        listOf("추석", "한가위", "추석 연휴", "추석 전날", "추석 다음날").forEach { raw -> assertEquals("추석", HolidayName.normalize(raw)) }
    }

    @Test
    fun `선거 이름에서 대수를 뗀다`() {
        assertEquals("대통령 선거", HolidayName.normalize("제21대 대통령 선거"))
        assertEquals("국회의원 선거", HolidayName.normalize("제21대 국회의원선거"))
        assertEquals("국회의원 선거", HolidayName.normalize("국회의원선거일"))
        assertEquals("전국 동시 지방 선거", HolidayName.normalize("동시지방선거일"))
        assertEquals("전국 동시 지방 선거", HolidayName.normalize("지방 선거일"))
    }

    @Test
    fun `알 수 없는 이름은 공백만 정리해 그대로 둔다`() {
        assertEquals("정월대보름", HolidayName.normalize("정월대보름"))
        assertEquals("국군의 날", HolidayName.normalize("  국군의  날 "))
    }
}
