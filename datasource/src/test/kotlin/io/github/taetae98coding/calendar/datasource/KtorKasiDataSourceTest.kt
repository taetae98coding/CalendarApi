package io.github.taetae98coding.calendar.datasource

import io.github.taetae98coding.calendar.datasource.http.Throttle
import io.github.taetae98coding.calendar.datasource.kasi.KasiApi
import io.github.taetae98coding.calendar.datasource.kasi.KasiService
import io.github.taetae98coding.calendar.datasource.kasi.KtorKasiDataSource
import io.github.taetae98coding.calendar.datasource.kasi.OpenApiException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.YearMonth

class KtorKasiDataSourceTest {
    private val yearMonth = YearMonth(2025, 1)

    private fun dataSource(status: HttpStatusCode = HttpStatusCode.OK, body: String, onRequest: (String) -> Unit = {}): KtorKasiDataSource {
        val engine = MockEngine { request ->
            onRequest(request.url.toString())
            respond(body, status, headersOf("Content-Type", ContentType.Application.Json.toString()))
        }

        return KtorKasiDataSource(KtorKasiDataSource.client("KEY", engine), Throttle(maxConcurrency = 1, requestsPerSecond = 1000))
    }

    private fun envelope(code: String = "00", items: String = "\"\"", count: Int = 0): String {
        return """{"response":{"header":{"resultCode":"$code","resultMsg":"-"},"body":{"items":$items,"numOfRows":100,"pageNo":1,"totalCount":$count}}}"""
    }

    @Test
    fun `서비스 경로와 인증키, 연월을 붙여 요청한다`() = runTest {
        var url = ""

        dataSource(body = envelope(), onRequest = { url = it }).get(KasiApi.REST_DE, yearMonth)

        assertTrue(url.startsWith("https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo?"), url)
        assertTrue("serviceKey=KEY" in url)
        assertTrue("solYear=2025" in url)
        assertTrue("solMonth=01" in url)
    }

    @Test
    fun `항목이 있으면 원본을 그대로 돌려준다`() = runTest {
        val body = envelope(items = """{"item":{"dateKind":"01","dateName":"1월1일","isHoliday":"Y","locdate":20250101}}""", count = 1)

        val raw = dataSource(body = body).get(KasiApi.REST_DE, yearMonth).getOrThrow()

        assertNotNull(raw)
        assertEquals(1, raw.jsonObjectPath("response", "body", "totalCount"))
    }

    @Test
    fun `404 와 자료 없음, 0건은 성공이면서 null 이다`() = runTest {
        assertNull(dataSource(HttpStatusCode.NotFound, "").get(KasiApi.REST_DE, yearMonth).getOrThrow())
        assertNull(dataSource(body = envelope(code = "03")).get(KasiApi.REST_DE, yearMonth).getOrThrow())
        assertNull(dataSource(body = envelope(count = 0)).get(KasiApi.LUN_CAL, yearMonth).getOrThrow())
    }

    @Test
    fun `오류 봉투는 실패로 돌려주고 코드를 보존한다`() = runTest {
        val result = dataSource(body = envelope(code = "22")).get(KasiApi.REST_DE, yearMonth)

        val exception = result.exceptionOrNull()

        assertTrue(exception is OpenApiException && exception.isQuotaExceeded, "$exception")
    }

    @Test
    fun `활용신청 여부는 미등록 오류일 때만 거짓이다`() = runTest {
        val notRegistered = """{"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"errMsg":"SERVICE_KEY_IS_NOT_REGISTERED_ERROR","returnAuthMsg":"-","returnReasonCode":"30"}}}"""

        assertFalse(dataSource(body = notRegistered).isRegistered(KasiService.SPCDE))
        assertTrue(dataSource(body = envelope()).isRegistered(KasiService.LUNAR))
        assertTrue(dataSource(body = envelope(code = "22")).isRegistered(KasiService.LUNAR))
    }

    private fun kotlinx.serialization.json.JsonElement.jsonObjectPath(vararg path: String): Int {
        return path.fold(this) { element, key -> (element as kotlinx.serialization.json.JsonObject).getValue(key) }
            .let { element -> (element as kotlinx.serialization.json.JsonPrimitive).content.toInt() }
    }
}
