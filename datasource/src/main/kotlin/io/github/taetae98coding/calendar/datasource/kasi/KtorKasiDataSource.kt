package io.github.taetae98coding.calendar.datasource.kasi

import io.github.taetae98coding.calendar.datasource.SourceException
import io.github.taetae98coding.calendar.datasource.http.HttpClients
import io.github.taetae98coding.calendar.datasource.http.Throttle
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.takeFrom
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.serialization.json.JsonElement

class KtorKasiDataSource(
    private val client: HttpClient,
    private val throttle: Throttle,
) : KasiDataSource {
    override suspend fun get(api: KasiApi, yearMonth: YearMonth): Result<JsonElement?> {
        val description = "KASI ${api.route} $yearMonth"

        return runCatching {
            val response = throttle.withPermit {
                client.get(api.route) {
                    parameter("solYear", yearMonth.year.toString().padStart(4, '0'))
                    parameter("solMonth", yearMonth.month.number.toString().padStart(2, '0'))
                }
            }

            if (response.status == HttpStatusCode.NotFound) return@runCatching null

            val raw = runCatching { response.body<JsonElement>() }
                .getOrElse { throwable -> throw IllegalStateException("$description 응답 해석 실패. status=${response.status}", throwable) }

            // 오류 응답을 캐시에 남기지 않도록 여기서 검증한다. 원본은 그대로 돌려준다.
            val body = try {
                KasiResponse.body(raw, description)
            } catch (exception: OpenApiException) {
                if (exception.isNoData) return@runCatching null

                throw exception
            }

            raw.takeIf { body.count > 0 }
        }
    }

    override suspend fun isRegistered(service: KasiService): Boolean {
        val result = get(service.probe, PROBE_YEAR_MONTH)

        return (result.exceptionOrNull() as? SourceException)?.isNotRegistered != true
    }

    companion object {
        private const val BASE_URL = "https://apis.data.go.kr/B090041/openapi/service/"

        /** 활용신청 여부를 확인할 때 쓰는 아무 월. 두 서비스 모두 제공하는 구간이면 된다. */
        private val PROBE_YEAR_MONTH = YearMonth(2025, 1)

        fun client(serviceKey: String, engine: HttpClientEngine = OkHttp.create()): HttpClient {
            return HttpClients.create(engine) {
                install(DefaultRequest) {
                    url.takeFrom(BASE_URL)
                    url.parameters.append("serviceKey", serviceKey)
                    url.parameters.append("_type", "json")
                    url.parameters.append("numOfRows", "100")
                }
            }
        }
    }
}
