package io.github.taetae98coding.calendar.openapi.nager

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json

/**
 * Nager.Date 공개 공휴일 API 클라이언트. 인증키가 필요 없으며 1975 ~ 2076 년을 제공한다.
 */
data object NagerDataSource {
    private const val BASE_URL = "https://date.nager.at/api/v3/PublicHolidays"

    const val MIN_YEAR = 1975
    const val MAX_YEAR = 2076

    private val semaphore = Semaphore(4)

    private val apiJson by lazy {
        Json {
            ignoreUnknownKeys = true
        }
    }

    private val client by lazy { HttpClient { expectSuccess = false } }

    suspend fun getHoliday(year: Int, countryCode: String): List<NagerHoliday> {
        if (year !in MIN_YEAR..MAX_YEAR) return emptyList()

        val (status, text) = semaphore.withPermit {
            val response = client.get("$BASE_URL/$year/$countryCode")

            response.status to response.bodyAsText()
        }

        if (status == HttpStatusCode.NotFound) return emptyList()
        if (status != HttpStatusCode.OK) error("Nager.Date $year/$countryCode 실패. status=$status, body=${text.take(500)}")

        return apiJson.decodeFromString(text)
    }
}
