package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.openapi.OpenApiClient
import io.github.taetae98coding.calendar.openapi.entity.OpenApiResult
import io.github.taetae98coding.calendar.openapi.kasi.KasiBody
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OpenApiResultTest {
    private fun decode(body: String) = OpenApiClient.json.decodeFromString<OpenApiResult<KasiBody>>(body)

    @Test
    fun `정상 응답이면 본문을 돌려준다`() {
        val result = decode(
            """{"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},"body":{"items":"","numOfRows":100,"pageNo":1,"totalCount":0}}}""",
        )

        assertEquals(0, result.bodyOrThrow("테스트").count)
    }

    @Test
    fun `인증키 오류 봉투를 같은 타입으로 받아 메시지로 바꾼다`() {
        val result = decode(
            """{"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"errMsg":"SERVICE_KEY_IS_NOT_REGISTERED_ERROR","returnAuthMsg":"등록되지 않은 서비스키","returnReasonCode":"30"}}}""",
        )

        val exception = assertFailsWith<IllegalStateException> { result.bodyOrThrow("테스트") }

        assertTrue(exception.message.orEmpty().contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR"))
        assertTrue(exception.message.orEmpty().contains("30"))
    }

    @Test
    fun `헤더 코드가 성공이 아니면 실패로 본다`() {
        val result = decode(
            """{"response":{"header":{"resultCode":"22","resultMsg":"LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR"},"body":{"items":"","numOfRows":100,"pageNo":1,"totalCount":0}}}""",
        )

        val exception = assertFailsWith<IllegalStateException> { result.bodyOrThrow("테스트") }

        assertTrue(exception.message.orEmpty().contains("LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR"))
    }
}
