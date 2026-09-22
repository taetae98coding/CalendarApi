package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.calendar.CalendarUpdater
import io.github.taetae98coding.calendar.holiday.HolidayUpdater
import io.github.taetae98coding.calendar.lunar.LunarUpdater
import io.github.taetae98coding.calendar.meta.MetaWriter
import kotlin.time.TimeSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun main() {
    require(!System.getenv("SERVICE_KEY").isNullOrBlank()) { "SERVICE_KEY(공공데이터포털 인증키) 환경 변수가 필요합니다." }

    val config = Config.fromEnvironment()
    val start = TimeSource.Monotonic.markNow()

    println("[CalendarApi] 공휴일 ${config.startYear} ~ ${config.endInclusiveYear}, 음력 ${config.lunarStartYear} ~ ${config.lunarEndInclusiveYear} (예산 ${config.lunarFetchBudget}월)")

    coroutineScope {
        // 특일 정보와 음양력 정보는 서로 다른 서비스라 트래픽 한도가 별도로 잡힌다. 함께 수집한다.
        val holidayJob = async { HolidayUpdater.update(config) }
        val lunarJob = async { LunarUpdater.update(config) }

        val holidays = holidayJob.await()
        println("[CalendarApi] 공휴일 생성 완료 (${start.elapsedNow()})")

        val lunarResult = lunarJob.await()
        println("[CalendarApi] 음력 생성 완료 (${start.elapsedNow()})")

        launch { CalendarUpdater.update(config, holidays) }
        launch { MetaWriter.write(config, lunarResult) }
        launch { IndexPage.write(config) }
    }

    println("[CalendarApi] 전체 완료 (${start.elapsedNow()})")
}
