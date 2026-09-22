package io.github.taetae98coding.calendar.datasource.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/** 원천 API 클라이언트가 공유하는 Ktor 설정. 엔진을 바꿔 끼울 수 있어 테스트에서는 MockEngine 을 쓴다. */
object HttpClients {
    private const val MAX_RETRIES = 5

    /** 원천 응답은 필드가 자주 늘고 숫자가 문자열로 오기도 해서 느슨하게 읽는다. */
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun create(engine: HttpClientEngine = OkHttp.create(), block: HttpClientConfig<*>.() -> Unit = {}): HttpClient {
        return HttpClient(engine) {
            // 오류 응답도 본문을 읽어 원인을 알려주기 위해 예외로 바꾸지 않는다.
            expectSuccess = false

            install(ContentNegotiation) {
                json(json)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30.seconds.inWholeMilliseconds
                connectTimeoutMillis = 10.seconds.inWholeMilliseconds
            }

            install(HttpRequestRetry) {
                // 429(요청 속도 초과)는 잠시 뒤 다시 하면 되는 일시적 실패다. 4xx 라 기본 재시도 대상이 아니므로 직접 지정한다.
                retryIf(maxRetries = MAX_RETRIES) { _, response ->
                    response.status == HttpStatusCode.TooManyRequests || response.status.value >= 500
                }
                retryOnExceptionIf(maxRetries = MAX_RETRIES) { _, cause -> cause !is CancellationException }
                exponentialDelay(base = 2.0, maxDelayMs = 30.seconds.inWholeMilliseconds)
            }

            block()
        }
    }
}
