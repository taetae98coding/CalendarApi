package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.data.cache.UpdateScheduler
import io.github.taetae98coding.calendar.data.holiday.DefaultHolidayRepository
import io.github.taetae98coding.calendar.data.lunar.DefaultLunarRepository
import io.github.taetae98coding.calendar.datasource.openapi.kasi.KasiDataSource
import io.github.taetae98coding.calendar.domain.CalendarYears
import io.github.taetae98coding.calendar.publish.DocsUpdater
import io.github.taetae98coding.calendar.publish.IndexPage
import io.github.taetae98coding.calendar.publish.MetaWriter
import io.github.taetae98coding.calendar.publish.Pruner
import kotlin.time.TimeSource

suspend fun main() {
    require(!System.getenv("DATA_GO_KR_SERVICE_KEY").isNullOrBlank()) {
        "DATA_GO_KR_SERVICE_KEY(공공데이터포털 일반 인증키 Decoding)가 없습니다. secrets.properties.example 을 secrets.properties 로 복사해 채우거나 DATA_GO_KR_SERVICE_KEY 환경 변수를 설정하세요."
    }

    val config = Config.fromEnvironment()
    val start = TimeSource.Monotonic.markNow()

    println("[CalendarApi] ${CalendarYears.START_YEAR} ~ ${CalendarYears.END_INCLUSIVE_YEAR} (서비스당 예산 ${config.fetchBudget}건)")

    // 인증키는 하나지만 활용신청은 서비스 단위다. 미신청 서비스에 수천 번 호출하지 않도록 먼저 확인한다.
    val registrations = KasiDataSource.checkRegistrations()
    registrations.filterValues { isRegistered -> !isRegistered }
        .keys
        .forEach { service -> println("[CalendarApi] '${service.displayName}' 가 활용신청되지 않았습니다. 신청 : ${service.applyUrl}") }

    // 범위나 폴더 구조가 바뀌었을 때 남는 파일을 먼저 정리한다.
    Pruner.prune()

    // 갱신은 도메인 단위로 건다. 예산·순서·동시성은 UpdateScheduler 가 서비스 단위로 묶어 관리한다.
    UpdateScheduler.update(
        updaters = listOf(DefaultHolidayRepository, DefaultLunarRepository),
        fetchBudget = config.fetchBudget,
        registrations = registrations,
    )
    println("[CalendarApi] 캐시 갱신 완료 (${start.elapsedNow()})")

    // 배포 문서는 이번 실행에서 무엇을 받았는지와 무관하게 언제나 캐시 전체를 다시 읽어 만든다.
    DocsUpdater.update(DefaultHolidayRepository, DefaultLunarRepository)
    println("[CalendarApi] 문서 생성 완료 (${start.elapsedNow()})")

    IndexPage.write(MetaWriter.write())

    println("[CalendarApi] 전체 완료 (${start.elapsedNow()})")
}
