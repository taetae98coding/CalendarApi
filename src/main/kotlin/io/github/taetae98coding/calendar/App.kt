package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.calendar.CalendarUpdater
import io.github.taetae98coding.calendar.holiday.HolidayUpdater
import io.github.taetae98coding.calendar.lunar.LunarUpdater
import io.github.taetae98coding.calendar.meta.MetaWriter
import io.github.taetae98coding.calendar.openapi.kasi.KasiDataSource
import io.github.taetae98coding.calendar.openapi.kasi.KasiService
import kotlin.time.TimeSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun main() {
    require(!System.getenv("DATA_GO_KR_SERVICE_KEY").isNullOrBlank()) {
        "DATA_GO_KR_SERVICE_KEY(공공데이터포털 일반 인증키 Decoding)가 없습니다. secrets.properties.example 을 secrets.properties 로 복사해 채우거나 DATA_GO_KR_SERVICE_KEY 환경 변수를 설정하세요."
    }

    val config = Config.fromEnvironment()
    val start = TimeSource.Monotonic.markNow()

    println("[CalendarApi] 공휴일 ${config.startYear} ~ ${config.endInclusiveYear}, 음력 ${config.lunarStartYear} ~ ${config.lunarEndInclusiveYear} (예산 ${config.lunarFetchBudget}월)")

    // 인증키는 하나지만 활용신청은 서비스 단위다. 미신청 서비스에 수천 번 호출하지 않도록 먼저 확인한다.
    val registrations = KasiDataSource.checkRegistrations()
    registrations.filterValues { isRegistered -> !isRegistered }
        .keys
        .forEach { service -> println("[CalendarApi] '${service.displayName}' 가 활용신청되지 않았습니다. 신청 : ${service.applyUrl}") }

    coroutineScope {
        // 특일 정보와 음양력 정보는 서로 다른 서비스라 트래픽 한도가 별도로 잡힌다. 함께 수집한다.
        val holidayJob = async { HolidayUpdater.update(config, registrations.getValue(KasiService.SPCDE)) }
        val lunarJob = async { LunarUpdater.update(config, registrations.getValue(KasiService.LUNAR)) }

        val holidays = holidayJob.await()
        println("[CalendarApi] 공휴일 생성 완료 (${start.elapsedNow()})")

        val lunarResult = lunarJob.await()
        println("[CalendarApi] 음력 생성 완료 ${lunarResult.completedYears.size}년, 남은 연도 ${lunarResult.missingYears.size}년 (${start.elapsedNow()})")

        CalendarUpdater.update(config, holidays)
    }

    // 배포되어 있는 파일을 그대로 훑어 범위를 적는다. 범위를 좁혀 실행해도 문서가 줄어들지 않는다.
    IndexPage.write(MetaWriter.write())

    println("[CalendarApi] 전체 완료 (${start.elapsedNow()})")
}
