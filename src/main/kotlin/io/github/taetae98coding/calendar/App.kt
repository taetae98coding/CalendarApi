package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.calendar.CalendarUpdater
import io.github.taetae98coding.calendar.holiday.HolidayUpdater
import io.github.taetae98coding.calendar.lunar.LunarUpdater
import io.github.taetae98coding.calendar.meta.MetaWriter
import kotlin.time.TimeSource

suspend fun main() {
    require(!System.getenv("SERVICE_KEY").isNullOrBlank()) { "SERVICE_KEY(공공데이터포털 인증키) 환경 변수가 필요합니다." }

    val config = Config.fromEnvironment()
    val start = TimeSource.Monotonic.markNow()

    println("[CalendarApi] 공휴일 ${config.startYear} ~ ${config.endInclusiveYear}, 음력 ${config.lunarStartYear} ~ ${config.lunarEndInclusiveYear} (예산 ${config.lunarFetchBudget}월)")

    val holidays = HolidayUpdater.update(config)
    println("[CalendarApi] 공휴일 생성 완료 (${start.elapsedNow()})")

    val lunarResult = LunarUpdater.update(config)
    println("[CalendarApi] 음력 생성 완료 (${start.elapsedNow()})")

    CalendarUpdater.update(config, holidays)
    MetaWriter.write(config, lunarResult)
    IndexPage.write(config)

    println("[CalendarApi] 전체 완료 (${start.elapsedNow()})")
}
