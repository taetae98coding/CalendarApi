package io.github.taetae98coding.calendar.datasource.http

import io.github.taetae98coding.calendar.datasource.SourceJson
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

/**
 * 원천 API 클라이언트가 공유하는 Ktor 설정.
 *
 * [engine] 을 넘기지 않으면 OkHttp 엔진을 클라이언트가 직접 만들어 소유하므로 [HttpClient.close] 가 엔진까지 닫는다.
 * 엔진 인스턴스를 넘기면(테스트의 MockEngine) Ktor 규칙대로 넘긴 쪽이 소유하고 닫는다.
 * 운영 경로에서 이 구분이 중요하다. OkHttp 의 스레드 풀은 데몬이 아니라서 닫히지 않으면 main 이 끝나고도 프로세스가 1분 가까이 남는다.
 */
object HttpClients {
    private const val MAX_RETRIES = 5

    fun create(engine: HttpClientEngine? = null, block: HttpClientConfig<*>.() -> Unit = {}): HttpClient {
        val config: HttpClientConfig<*>.() -> Unit = {
            // 오류 응답도 본문을 읽어 원인을 알려주기 위해 예외로 바꾸지 않는다.
            expectSuccess = false

            install(ContentNegotiation) {
                json(SourceJson.lenient)
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

        return if (engine == null) HttpClient(OkHttp, config) else HttpClient(engine, config)
    }
}
