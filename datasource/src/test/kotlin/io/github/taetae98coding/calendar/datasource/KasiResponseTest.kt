package io.github.taetae98coding.calendar.datasource

import io.github.taetae98coding.calendar.datasource.kasi.KasiLunarItem
import io.github.taetae98coding.calendar.datasource.kasi.KasiResponse
import io.github.taetae98coding.calendar.datasource.kasi.KasiSpcdeItem
import io.github.taetae98coding.calendar.datasource.kasi.OpenApiException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class KasiResponseTest {
    private fun raw(body: String): JsonElement = Json.parseToJsonElement(body)

    private fun lunarItems(body: String): List<KasiLunarItem> = KasiResponse.items(raw(body), "테스트")

    private fun spcdeItems(body: String): List<KasiSpcdeItem> = KasiResponse.items(raw(body), "테스트")

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
    fun `그레고리력 이전 날짜는 율리우스 적일로 변환한다`() {
        // KASI 는 1582-10-15 이전을 율리우스력으로 준다. 1500-02-29 는 율리우스력에만 있는 날짜다.
        val items = lunarItems(
            """
            {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},"body":{"items":{"item":
            {"lunDay":"01","lunLeapmonth":"평","lunMonth":"02","lunYear":1500,"solDay":29,"solJd":2268992,"solMonth":"02","solYear":1500}
            },"numOfRows":100,"pageNo":1,"totalCount":1}}}
            """.trimIndent(),
        )

        assertEquals(LocalDate(1500, 3, 10), items[0].solarDate)
        assertEquals(1500, items[0].lunarYearValue)
        assertEquals(2, items[0].lunarMonthValue)
        assertEquals(1, items[0].lunarDayValue)
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
        assertEquals("1월1일", items[0].name)
        assertTrue(items[0].isHoliday)
        assertEquals(LocalDate(2025, 1, 5), items[1].date)
        assertFalse(items[1].isHoliday)
    }

    @Test
    fun `인증키 오류 봉투를 같은 타입으로 받아 예외로 바꾼다`() {
        val exception = assertFailsWith<OpenApiException> {
            KasiResponse.body(
                raw("""{"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"errMsg":"SERVICE_KEY_IS_NOT_REGISTERED_ERROR","returnAuthMsg":"등록되지 않은 서비스키","returnReasonCode":"30"}}}"""),
                "테스트",
            )
        }

        assertTrue(exception.isNotRegistered)
        assertTrue(exception.message.contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR"))
    }

    @Test
    fun `헤더 코드로 트래픽 초과와 자료 없음을 구분한다`() {
        fun failure(code: String, message: String): OpenApiException {
            return assertFailsWith {
                KasiResponse.body(
                    raw("""{"response":{"header":{"resultCode":"$code","resultMsg":"$message"},"body":{"items":"","numOfRows":100,"pageNo":1,"totalCount":0}}}"""),
                    "테스트",
                )
            }
        }

        assertTrue(failure("22", "LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR").isQuotaExceeded)
        assertTrue(failure("23", "LIMITED_NUMBER_OF_SERVICE_REQUESTS_PER_SECOND_EXCEEDS_ERROR").isRateLimited)
        assertTrue(failure("03", "NODATA_ERROR").isNoData)
    }
}
