package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.core.Logger
import io.github.taetae98coding.calendar.data.cache.CachePaths
import io.github.taetae98coding.calendar.data.cache.CachePruner
import io.github.taetae98coding.calendar.data.cache.CacheReader
import io.github.taetae98coding.calendar.data.cache.CacheStore
import io.github.taetae98coding.calendar.data.holiday.CachedHolidayRepository
import io.github.taetae98coding.calendar.data.lunar.CachedLunarRepository
import io.github.taetae98coding.calendar.data.source.RemoteSourceFetcher
import io.github.taetae98coding.calendar.data.source.SourceFetcher
import io.github.taetae98coding.calendar.data.update.CacheUpdater
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
import kotlinx.coroutines.CancellationException

/**
 * 계층을 조립하고 한 번의 실행을 순서대로 진행한다.
 *
 * 1. 범위 밖의 캐시와 문서를 지운다.
 * 2. 활용신청되지 않은 서비스를 확인해 건너뛰고, 오래된 구간부터 캐시를 갱신한다. 한도나 장애로 중간에 끝날 수 있다.
 * 3. 캐시 전체를 다시 읽어 문서를 만든다. 이번에 무엇을 받았는지와 무관하다.
 * 4. 생성 현황을 meta.json 과 index.html 에 적는다.
 *
 * 수집이 실패해도 배포는 진행한다. 문서는 **이번에 받은 것이 아니라 지금 가진 캐시 전체**로 만들기 때문에,
 * 갱신이 한 건도 안 됐어도 직전 상태의 문서는 그대로 나온다. 수집 실패로 배포까지 멈추면 이미 받아 둔 자료가 묻힌다.
 */
class CalendarApp(
    private val config: Config,
    private val logger: Logger = Logger.Stdout,
) {
    suspend fun run() {
        val start = TimeSource.Monotonic.markNow()
        logger.log("[CalendarApi] ${CalendarYears.START_YEAR} ~ ${CalendarYears.END_INCLUSIVE_YEAR}")

        val cachePaths = CachePaths(config.cacheRoot)
        val docPaths = DocPaths(config.docsRoot)
        val store = CacheStore(cachePaths)

        CachePruner(cachePaths, logger).prune()
        DocsPruner(docPaths, logger).prune()

        collect(store)
        logger.log("[CalendarApi] 캐시 갱신 단계 종료 (${start.elapsedNow()})")

        publish(store, docPaths)
        logger.log("[CalendarApi] 전체 완료 (${start.elapsedNow()})")
    }

    /**
     * 원천 -> 캐시. 받은 만큼만 남고, 못 받은 구간은 이전 갱신 시각을 유지해 다음 실행이 먼저 잡는다.
     *
     * 원천이 통째로 응답하지 않으면 여기서 예외가 나지만 삼킨다. 구간 하나의 실패는 CacheUpdater 가 안에서 받아낸다.
     * HTTP 클라이언트는 수집이 끝나면 닫는다. 열어 두면 OkHttp 스레드 풀이 남아 프로세스 종료가 늦어진다.
     */
    private suspend fun collect(store: CacheStore) {
        val kasiClient = KtorKasiDataSource.client(config.serviceKey)
        val nagerClient = KtorNagerDataSource.client()

        try {
            // 특일·음력 요청이 함께 돌기 때문에 호스트마다 하나의 제한을 공유한다.
            val fetcher = RemoteSourceFetcher(
                kasi = KtorKasiDataSource(kasiClient, throttle()),
                nager = KtorNagerDataSource(nagerClient, throttle()),
            )

            update(store, fetcher)
        } catch (exception: CancellationException) {
            // 취소는 실패가 아니라 바깥이 멈추라는 뜻이므로 삼키지 않는다.
            throw exception
        } catch (throwable: Throwable) {
            logger.log("[CalendarApi] 캐시 갱신이 중단됐습니다: ${throwable.message}. 지금 가진 캐시로 문서를 만듭니다.")
        } finally {
            kasiClient.close()
            nagerClient.close()
        }
    }

    private suspend fun update(store: CacheStore, fetcher: SourceFetcher) {
        val unavailable = fetcher.unavailable()
        unavailable.forEach { service -> logger.log("[CalendarApi] '${service.displayName}' 가 활용신청되지 않았습니다. 신청 : ${service.applyUrl}") }

        CacheUpdater(store, fetcher, logger).update(skip = unavailable)
    }

    /** 캐시 -> 저장소 -> 문서. 저장소는 캐시만 읽으므로 네트워크를 타지 않는다. */
    private suspend fun publish(store: CacheStore, docPaths: DocPaths) {
        val reader = CacheReader(store, logger)
        val holidayRepository = CachedHolidayRepository(reader)
        val lunarRepository = CachedLunarRepository(reader)

        val coverage = DocsUpdater(docPaths, holidayRepository, lunarRepository).update()
        val meta = MetaWriter(docPaths, CalendarSources).write(coverage)
        IndexPage(docPaths).write(meta)
    }

    private fun throttle(): Throttle = Throttle(config.maxConcurrency, config.maxRequestsPerSecond)
}
