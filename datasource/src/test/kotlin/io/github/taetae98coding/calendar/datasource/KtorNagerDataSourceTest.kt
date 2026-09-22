package io.github.taetae98coding.calendar.datasource

import io.github.taetae98coding.calendar.datasource.http.Throttle
import io.github.taetae98coding.calendar.datasource.nager.KtorNagerDataSource
import io.github.taetae98coding.calendar.datasource.nager.NagerResponse
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class KtorNagerDataSourceTest {
    private fun dataSource(status: HttpStatusCode = HttpStatusCode.OK, body: String, onRequest: (String) -> Unit = {}): KtorNagerDataSource {
        val engine = MockEngine { request ->
            onRequest(request.url.toString())
            respond(body, status, headersOf("Content-Type", ContentType.Application.Json.toString()))
        }

        return KtorNagerDataSource(KtorNagerDataSource.client(engine), Throttle(maxConcurrency = 1, requestsPerSecond = 1000))
    }

    @Test
    fun `연도와 국가 코드로 요청한다`() = runTest {
        var url = ""

        dataSource(body = "[]", onRequest = { url = it }).getHolidays(2026, "KR")

        assertEquals("https://date.nager.at/api/v3/PublicHolidays/2026/KR", url)
    }

    @Test
    fun `항목이 있으면 원본을 돌려주고 해석할 수 있다`() = runTest {
        val body = """[{"date":"2026-01-01","localName":"새해","name":"New Year's Day","countryCode":"KR","global":true,"counties":null,"types":["Public"]}]"""

        val raw = dataSource(body = body).getHolidays(2026, "KR").getOrThrow()

        assertNotNull(raw)
        assertEquals("새해", NagerResponse.holidays(raw).single().localName)
    }

    @Test
    fun `404 와 빈 배열은 성공이면서 null 이다`() = runTest {
        assertNull(dataSource(HttpStatusCode.NotFound, "").getHolidays(2051, "KR").getOrThrow())
        assertNull(dataSource(body = "[]").getHolidays(2026, "KR").getOrThrow())
    }

    @Test
    fun `그 외 오류 상태는 실패다`() = runTest {
        assertTrue(dataSource(HttpStatusCode.BadRequest, "").getHolidays(2026, "XX").isFailure)
    }
}
