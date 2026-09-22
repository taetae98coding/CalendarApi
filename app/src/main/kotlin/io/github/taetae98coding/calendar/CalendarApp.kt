package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.data.Logger
import io.github.taetae98coding.calendar.data.cache.CachePaths
import io.github.taetae98coding.calendar.data.cache.CachePruner
import io.github.taetae98coding.calendar.data.cache.CacheStore
import io.github.taetae98coding.calendar.data.cache.CacheUpdater
import io.github.taetae98coding.calendar.data.holiday.DefaultHolidayRepository
import io.github.taetae98coding.calendar.data.lunar.DefaultLunarRepository
import io.github.taetae98coding.calendar.data.source.RemoteSourceFetcher
import io.github.taetae98coding.calendar.datasource.http.Throttle
import io.github.taetae98coding.calendar.datasource.kasi.KtorKasiDataSource
import io.github.taetae98coding.calendar.datasource.nager.KtorNagerDataSource
import io.github.taetae98coding.calendar.domain.CalendarYears
import io.github.taetae98coding.calendar.publish.DocPaths
import io.github.taetae98coding.calendar.publish.DocsPruner
import io.github.taetae98coding.calendar.publish.DocsUpdater
import io.github.taetae98coding.calendar.publish.IndexPage
import io.github.taetae98coding.calendar.publish.MetaWriter
import kotlin.time.TimeSource

/**
 * 계층을 조립하고 한 번의 실행을 순서대로 진행한다.
 *
 * 1. 활용신청되지 않은 서비스를 확인해 건너뛴다.
 * 2. 범위 밖의 캐시와 문서를 지운다.
 * 3. 예산 안에서 캐시를 갱신한다.
 * 4. 캐시 전체를 다시 읽어 문서를 만든다. 이번에 무엇을 받았는지와 무관하다.
 * 5. 생성 현황을 meta.json 과 index.html 에 적는다.
 */
class CalendarApp(
    private val config: Config,
    private val logger: Logger = Logger.Stdout,
) {
    suspend fun run() {
        val start = TimeSource.Monotonic.markNow()
        logger.log("[CalendarApi] ${CalendarYears.START_YEAR} ~ ${CalendarYears.END_INCLUSIVE_YEAR} (서비스당 예산 ${config.fetchBudget}건)")

        val cachePaths = CachePaths(config.cacheRoot)
        val store = CacheStore(cachePaths)
        val holidayRepository = DefaultHolidayRepository(store, logger)
        val lunarRepository = DefaultLunarRepository(store, logger)

        // 특일·음력 요청이 함께 돌기 때문에 호스트마다 하나의 제한을 공유한다.
        val fetcher = RemoteSourceFetcher(
            kasi = KtorKasiDataSource(KtorKasiDataSource.client(config.serviceKey), throttle()),
            nager = KtorNagerDataSource(KtorNagerDataSource.client(), throttle()),
        )

        val unavailable = fetcher.unavailable()
        unavailable.forEach { service -> logger.log("[CalendarApi] '${service.displayName}' 가 활용신청되지 않았습니다. 신청 : ${service.applyUrl}") }

        val docPaths = DocPaths(config.docsRoot)
        CachePruner(cachePaths, logger).prune()
        DocsPruner(docPaths, logger).prune()

        CacheUpdater(store, fetcher, logger).update(config.fetchBudget, skip = unavailable)
        logger.log("[CalendarApi] 캐시 갱신 완료 (${start.elapsedNow()})")

        val coverage = DocsUpdater(docPaths, holidayRepository, lunarRepository).update()
        logger.log("[CalendarApi] 문서 생성 완료 (${start.elapsedNow()})")

        val meta = MetaWriter(docPaths).write(coverage)
        IndexPage(docPaths).write(meta)
        logger.log("[CalendarApi] 전체 완료 (${start.elapsedNow()})")
    }

    private fun throttle(): Throttle = Throttle(config.maxConcurrency, config.maxRequestsPerSecond)
}
