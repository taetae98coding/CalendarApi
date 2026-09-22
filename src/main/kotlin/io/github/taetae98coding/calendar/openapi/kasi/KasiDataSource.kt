package io.github.taetae98coding.calendar.openapi.kasi

import io.github.taetae98coding.calendar.openapi.OpenApiClient
import io.github.taetae98coding.calendar.openapi.entity.OpenApiException
import io.github.taetae98coding.calendar.openapi.entity.OpenApiResult
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.takeFrom
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * 한국천문연구원 OpenAPI 클라이언트.
 *
 * - 특일 정보: `SpcdeInfoService`
 * - 음양력 정보: `LrsrCldInfoService` (1391-02-05 ~ 2050-12-31)
 *
 * 응답을 도메인 모델로 좁히지 않고 [JsonElement] 그대로 돌려준다.
 * 호출자가 원본을 캐시에 남기고, 필요한 필드만 뽑는 일은 [parseItems] 에서 따로 한다.
 */
data object KasiDataSource {
    private const val BASE_URL = "https://apis.data.go.kr/B090041/openapi/service/"

    /** 활용신청 여부를 확인할 때 쓰는 아무 월. 두 서비스 모두 제공하는 구간이면 된다. */
    private val probeYearMonth = YearMonth(2025, 1)

    /** 특일·음력 요청이 함께 돌기 때문에 data.go.kr 로 나가는 총 동시 요청 수를 여기서 묶어 제한한다. */
    private val semaphore = Semaphore(OpenApiClient.maxConcurrency)

    private val client by lazy {
        OpenApiClient.create {
            install(DefaultRequest) {
                url.takeFrom(BASE_URL)
                url.parameters.append("serviceKey", System.getenv("DATA_GO_KR_SERVICE_KEY").orEmpty())
                url.parameters.append("_type", "json")
                url.parameters.append("numOfRows", "100")
            }
        }
    }

    suspend fun getSpcde(api: String, yearMonth: YearMonth): JsonElement {
        return get("${KasiService.SPCDE.path}/$api", yearMonth)
    }

    /** solDay 를 생략하면 해당 양력 월 전체의 음력 정보를 한 번에 받는다. */
    suspend fun getLunar(yearMonth: YearMonth): JsonElement {
        return get("${KasiService.LUNAR.path}/getLunCalInfo", yearMonth)
    }

    /** 캐시에 저장해 둔 원본이든 방금 받은 응답이든 같은 방식으로 항목을 뽑는다. */
    inline fun <reified T> parseItems(raw: JsonElement, description: String): List<T> {
        return OpenApiClient.json.decodeFromJsonElement<OpenApiResult<KasiBody>>(raw)
            .bodyOrThrow(description)
            .toItemList(OpenApiClient.json)
    }

    /**
     * 서비스마다 활용신청을 따로 해야 하므로, 본 수집을 시작하기 전에 한 번씩만 찔러 본다.
     * 신청되지 않은 서비스에 수천 번 호출하는 것을 막는다.
     */
    suspend fun checkRegistrations(): Map<KasiService, Boolean> {
        return coroutineScope {
            KasiService.entries.map { service -> async { service to isRegistered(service) } }
                .awaitAll()
                .toMap()
        }
    }

    private suspend fun isRegistered(service: KasiService): Boolean {
        val result = runCatching {
            when (service) {
                KasiService.SPCDE -> getSpcde("getRestDeInfo", probeYearMonth)
                KasiService.LUNAR -> getLunar(probeYearMonth)
            }
        }

        // 활용신청 문제가 아닌 일시적인 실패라면 본 수집에서 다시 판단하게 둔다.
        return result.exceptionOrNull().let { throwable -> (throwable as? OpenApiException)?.isNotRegistered != true }
    }

    private suspend fun get(path: String, yearMonth: YearMonth): JsonElement {
        val description = "KASI $path $yearMonth"

        val response = semaphore.withPermit {
            client.get(path) {
                parameter("solYear", yearMonth.year.toString().padStart(4, '0'))
                parameter("solMonth", yearMonth.month.number.toString().padStart(2, '0'))
            }
        }

        val raw = runCatching { response.body<JsonElement>() }
            .getOrElse { throwable -> throw IllegalStateException("$description 응답 해석 실패. status=${response.status}", throwable) }

        // 오류 응답을 캐시에 남기지 않도록 여기서 검증만 하고, 원본은 그대로 돌려준다.
        OpenApiClient.json.decodeFromJsonElement<OpenApiResult<KasiBody>>(raw).bodyOrThrow(description)

        return raw
    }
}
