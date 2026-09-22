package io.github.taetae98coding.calendar.data.source

import io.github.taetae98coding.calendar.data.cache.CachePeriod
import io.github.taetae98coding.calendar.datasource.kasi.KasiDataSource
import io.github.taetae98coding.calendar.datasource.kasi.KasiService
import io.github.taetae98coding.calendar.datasource.nager.NagerDataSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonElement

/** 실제 원천을 부른다. [Remote] 의 종류가 곧 어느 데이터소스를 쓰는지다. */
class RemoteSourceFetcher(
    private val kasi: KasiDataSource,
    private val nager: NagerDataSource,
) : SourceFetcher {
    override suspend fun fetch(api: SourceApi, period: CachePeriod): Result<JsonElement?> {
        return when (val remote = api.remote) {
            is Remote.Kasi -> kasi.get(remote.kasiService, remote.api, period.yearMonth)
            is Remote.Nager -> nager.getHolidays(period.year, remote.countryCode)
        }
    }

    /** 인증키는 하나지만 활용신청은 서비스 단위다. 서비스마다 한 번씩만 찔러 본다. */
    override suspend fun unavailable(): Set<FetchService> {
        return coroutineScope {
            KasiService.entries
                .map { service -> async { service.takeUnless { kasi.isRegistered(service) } } }
                .awaitAll()
                .filterNotNull()
                .map(FetchService::of)
                .toSet()
        }
    }
}
