package io.github.taetae98coding.calendar.openapi.kasi

import io.github.taetae98coding.calendar.openapi.entity.OpenApiResult
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.takeFrom
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.serialization.json.Json

/**
 * 한국천문연구원 OpenAPI 클라이언트.
 *
 * - 특일 정보: `SpcdeInfoService`
 * - 음양력 정보: `LrsrCldInfoService` (1391-02-05 ~ 2050-12-31)
 */
data object KasiDataSource {
    private const val BASE_URL = "https://apis.data.go.kr/B090041/openapi/service/"

    private val semaphore = Semaphore(4)

    val apiJson by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    private val client by lazy {
        HttpClient {
            expectSuccess = false

            install(DefaultRequest) {
                url.takeFrom(BASE_URL)
                url.parameters.append("serviceKey", System.getenv("SERVICE_KEY"))
                url.parameters.append("_type", "json")
            }
        }
    }

    suspend fun getSpcdeItems(api: String, yearMonth: YearMonth): List<KasiSpcdeItem> {
        val body = request("SpcdeInfoService/$api") {
            parameter("solYear", yearMonth.year)
            parameter("solMonth", yearMonth.month.number.toString().padStart(2, '0'))
            parameter("numOfRows", 100)
        }

        return body.toItemList(apiJson)
    }

    /** solDay 를 생략하면 해당 양력 월 전체의 음력 정보를 한 번에 받는다. */
    suspend fun getLunar(yearMonth: YearMonth): List<KasiLunarItem> {
        val body = request("LrsrCldInfoService/getLunCalInfo") {
            parameter("solYear", yearMonth.year.toString().padStart(4, '0'))
            parameter("solMonth", yearMonth.month.number.toString().padStart(2, '0'))
            parameter("numOfRows", 100)
        }

        return body.toItemList(apiJson)
    }

    private suspend fun request(path: String, block: HttpRequestBuilder.() -> Unit): KasiBody {
        val (status, text) = semaphore.withPermit {
            val response = client.get(path, block)

            response.status to response.bodyAsText()
        }

        val result = runCatching { apiJson.decodeFromString<OpenApiResult<KasiBody>>(text) }
            .getOrElse { throwable -> throw IllegalStateException("KASI $path 응답 해석 실패. status=$status, body=${text.take(500)}", throwable) }

        val header = result.response.header
        if (status != HttpStatusCode.OK || header.code != "00") {
            error("KASI $path 실패. status=$status, code=${header.code}, message=${header.message}")
        }

        return result.response.body
    }
}
