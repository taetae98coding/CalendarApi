package io.github.taetae98coding.calendar.openapi.nager

import io.github.taetae98coding.calendar.openapi.OpenApiClient
import io.github.taetae98coding.calendar.openapi.RateLimiter
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Nager.Date 공개 공휴일 API 클라이언트. 인증키가 필요 없다.
 *
 * KASI 와 마찬가지로 원본 [JsonElement] 를 돌려주고, 필드를 뽑는 일은 [parseHolidays] 에서 한다.
 */
data object NagerDataSource {
    private const val BASE_URL = "https://date.nager.at/api/v3/PublicHolidays/"

    private val semaphore = Semaphore(OpenApiClient.maxConcurrency)
    private val rateLimiter = RateLimiter(OpenApiClient.maxRequestsPerSecond)

    private val client by lazy {
        OpenApiClient.create {
            install(DefaultRequest) {
                url.takeFrom(BASE_URL)
            }
        }
    }

    /** 성공이면 응답 원본, 제공하지 않는 연도(404)면 null. 404 도 정상 응답으로 본다. */
    suspend fun getHoliday(year: Int, countryCode: String): Result<JsonElement?> {
        return runCatching {
            val response = semaphore.withPermit {
                rateLimiter.acquire()

                client.get("$year/$countryCode")
            }

            when {
                response.status == HttpStatusCode.NotFound -> null
                response.status.isSuccess() -> response.body<JsonElement>()
                else -> error("Nager.Date $year/$countryCode 실패. status=${response.status}")
            }
        }
    }

    fun parseHolidays(raw: JsonElement): List<NagerHoliday> {
        return OpenApiClient.json.decodeFromJsonElement(raw)
    }
}
