package io.github.taetae98coding.calendar.openapi.kasi

import io.github.taetae98coding.calendar.openapi.OpenApiClient
import io.github.taetae98coding.calendar.openapi.entity.OpenApiResult
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.takeFrom
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number

/**
 * 한국천문연구원 OpenAPI 클라이언트.
 *
 * - 특일 정보: `SpcdeInfoService`
 * - 음양력 정보: `LrsrCldInfoService` (1391-02-05 ~ 2050-12-31)
 */
data object KasiDataSource {
    private const val BASE_URL = "https://apis.data.go.kr/B090041/openapi/service/"

    /** 특일·음력 요청이 함께 돌기 때문에 data.go.kr 로 나가는 총 동시 요청 수를 여기서 묶어 제한한다. */
    private val semaphore = Semaphore(OpenApiClient.maxConcurrency)

    private val client by lazy {
        OpenApiClient.create {
            install(DefaultRequest) {
                url.takeFrom(BASE_URL)
                url.parameters.append("serviceKey", System.getenv("SERVICE_KEY").orEmpty())
                url.parameters.append("_type", "json")
                url.parameters.append("numOfRows", "100")
            }
        }
    }

    suspend fun getSpcdeItems(api: String, yearMonth: YearMonth): List<KasiSpcdeItem> {
        return getItems("SpcdeInfoService/$api", yearMonth)
    }

    /** solDay 를 생략하면 해당 양력 월 전체의 음력 정보를 한 번에 받는다. */
    suspend fun getLunarItems(yearMonth: YearMonth): List<KasiLunarItem> {
        return getItems("LrsrCldInfoService/getLunCalInfo", yearMonth)
    }

    private suspend inline fun <reified T> getItems(path: String, yearMonth: YearMonth): List<T> {
        val description = "KASI $path $yearMonth"

        val response = semaphore.withPermit {
            client.get(path) {
                parameter("solYear", yearMonth.year.toString().padStart(4, '0'))
                parameter("solMonth", yearMonth.month.number.toString().padStart(2, '0'))
            }
        }

        val result = runCatching { response.body<OpenApiResult<KasiBody>>() }
            .getOrElse { throwable -> throw IllegalStateException("$description 응답 해석 실패. status=${response.status}", throwable) }

        return result.bodyOrThrow(description).toItemList(OpenApiClient.json)
    }
}
