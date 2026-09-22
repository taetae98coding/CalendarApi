package io.github.taetae98coding.calendar.openapi

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json

/** 두 OpenAPI 클라이언트가 공유하는 Ktor 설정. */
data object OpenApiClient {
    private const val DEFAULT_MAX_CONCURRENCY = 8
    private const val MAX_RETRIES = 3

    /** 외부 API 한 곳으로 동시에 나갈 수 있는 요청 수. */
    val maxConcurrency: Int = System.getenv("MAX_CONCURRENCY")?.toIntOrNull() ?: DEFAULT_MAX_CONCURRENCY

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
                retryOnServerErrors(maxRetries = MAX_RETRIES)
                retryOnException(maxRetries = MAX_RETRIES, retryOnTimeout = true)
                exponentialDelay()
            }

            block()
        }
    }
}
