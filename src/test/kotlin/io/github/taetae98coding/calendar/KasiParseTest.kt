package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.openapi.OpenApiClient
import io.github.taetae98coding.calendar.openapi.entity.OpenApiResult
import io.github.taetae98coding.calendar.openapi.kasi.KasiBody
import io.github.taetae98coding.calendar.openapi.kasi.KasiLunarItem
import io.github.taetae98coding.calendar.openapi.kasi.KasiSpcdeItem
import io.github.taetae98coding.calendar.openapi.kasi.toItemList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class KasiParseTest {
    private val json = OpenApiClient.json

    private fun lunarItems(body: String): List<KasiLunarItem> {
        return json.decodeFromString<OpenApiResult<KasiBody>>(body).bodyOrThrow("테스트").toItemList(json)
    }

    private fun spcdeItems(body: String): List<KasiSpcdeItem> {
        return json.decodeFromString<OpenApiResult<KasiBody>>(body).bodyOrThrow("테스트").toItemList(json)
    }

    @Test
    fun `음력 응답이 배열이면 모든 항목을 읽는다`() {
        val items = lunarItems(
            """
            {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},"body":{"items":{"item":[
            {"lunAge":"","lunDay":"02","lunIljin":"계묘(癸卯)","lunLeapmonth":"평","lunMonth":"12","lunNday":30,"lunSecha":"갑진(甲辰)","lunWolgeon":"병자(丙子)","lunYear":"2024","solDay":"01","solJd":2460677,"solLeapyear":"평","solMonth":"01","solWeek":"수","solYear":"2025"},
            {"lunAge":"","lunDay":"03","lunIljin":"갑진(甲辰)","lunLeapmonth":"윤","lunMonth":"12","lunNday":30,"lunSecha":"갑진(甲辰)","lunWolgeon":"병자(丙子)","lunYear":"2024","solDay":"02","solJd":2460678,"solLeapyear":"평","solMonth":"01","solWeek":"목","solYear":"2025"}
            ]},"numOfRows":100,"pageNo":1,"totalCount":2}}}
            """.trimIndent(),
        )

        assertEquals(2, items.size)
        assertEquals(LocalDate(2025, 1, 1), items[0].solarDate)
        assertEquals(2024, items[0].lunarYearValue)
        assertEquals(12, items[0].lunarMonthValue)
        assertEquals(2, items[0].lunarDayValue)
        assertFalse(items[0].isLeapMonth)
        assertTrue(items[1].isLeapMonth)
    }

    @Test
    fun `음력 응답이 단일 객체여도 읽는다`() {
        val items = lunarItems(
            """
            {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},"body":{"items":{"item":
            {"lunDay":"02","lunLeapmonth":"평","lunMonth":"12","lunYear":"2024","solDay":"01","solMonth":"01","solYear":"2025"}
            },"numOfRows":100,"pageNo":1,"totalCount":1}}}
            """.trimIndent(),
        )

        assertEquals(1, items.size)
        assertEquals(LocalDate(2025, 1, 1), items[0].solarDate)
    }

    @Test
    fun `숫자 필드가 문자열이 아니어도 읽는다`() {
        val items = lunarItems(
            """
            {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},"body":{"items":{"item":
            {"lunDay":2,"lunLeapmonth":"평","lunMonth":12,"lunYear":2024,"solDay":1,"solMonth":1,"solYear":2025}
            },"numOfRows":100,"pageNo":1,"totalCount":1}}}
            """.trimIndent(),
        )

        assertEquals(LocalDate(2025, 1, 1), items[0].solarDate)
        assertEquals(12, items[0].lunarMonthValue)
    }

    @Test
    fun `결과가 없으면 빈 목록이다`() {
        val items = lunarItems(
            """{"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},"body":{"items":"","numOfRows":100,"pageNo":1,"totalCount":0}}}""",
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun `특일 정보를 읽는다`() {
        val items = spcdeItems(
            """
            {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},"body":{"items":{"item":[
            {"dateKind":"01","dateName":"1월1일","isHoliday":"Y","locdate":20250101,"seq":1},
            {"dateKind":"03","dateName":"소한","isHoliday":"N","locdate":20250105,"seq":1}
            ]},"numOfRows":100,"pageNo":1,"totalCount":2}}}
            """.trimIndent(),
        )

        assertEquals(2, items.size)
        assertEquals("신정", items[0].prettyName)
        assertTrue(items[0].isHoliday)
        assertEquals(LocalDate(2025, 1, 5), items[1].date)
        assertFalse(items[1].isHoliday)
    }
}
