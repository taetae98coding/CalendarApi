package io.github.taetae98coding.calendar.datasource.nager

import io.github.taetae98coding.calendar.datasource.http.HttpClients
import io.github.taetae98coding.calendar.datasource.http.Throttle
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import kotlinx.serialization.json.JsonElement

class KtorNagerDataSource(
    private val client: HttpClient,
    private val throttle: Throttle,
) : NagerDataSource {
    override suspend fun getHolidays(year: Int, countryCode: String): Result<JsonElement?> {
        return runCatching {
            val response = throttle.withPermit { client.get("$year/$countryCode") }

            when {
                response.status == HttpStatusCode.NotFound -> null
                response.status.isSuccess() -> response.body<JsonElement>().takeIf { raw -> NagerResponse.holidays(raw).isNotEmpty() }
                else -> error("Nager.Date $year/$countryCode 실패. status=${response.status}")
            }
        }
    }

    companion object {
        private const val BASE_URL = "https://date.nager.at/api/v3/PublicHolidays/"

        fun client(engine: HttpClientEngine = OkHttp.create()): HttpClient {
            return HttpClients.create(engine) {
                install(DefaultRequest) {
                    url.takeFrom(BASE_URL)
                }
            }
        }
    }
}
