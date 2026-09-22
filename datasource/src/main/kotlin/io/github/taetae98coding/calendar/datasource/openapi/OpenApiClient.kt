package io.github.taetae98coding.calendar.datasource.openapi

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json

/** 두 OpenAPI 클라이언트가 공유하는 Ktor 설정. */
data object OpenApiClient {
    private const val DEFAULT_MAX_CONCURRENCY = 8
    private const val DEFAULT_MAX_REQUESTS_PER_SECOND = 20
    private const val MAX_RETRIES = 5

    /** 외부 API 한 곳으로 동시에 나갈 수 있는 요청 수. */
    val maxConcurrency: Int = System.getenv("MAX_CONCURRENCY")?.toIntOrNull() ?: DEFAULT_MAX_CONCURRENCY

    /** 외부 API 한 곳으로 나가는 초당 요청 수. */
    val maxRequestsPerSecond: Int = System.getenv("MAX_REQUESTS_PER_SECOND")?.toIntOrNull() ?: DEFAULT_MAX_REQUESTS_PER_SECOND

    val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    fun create(block: HttpClientConfig<OkHttpConfig>.() -> Unit = {}): HttpClient {
        return HttpClient(OkHttp) {
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
                retryOnExceptionIf(maxRetries = MAX_RETRIES) { _, cause -> cause !is kotlinx.coroutines.CancellationException }
                exponentialDelay(base = 2.0, maxDelayMs = 30.seconds.inWholeMilliseconds)
            }

            block()
        }
    }
}
