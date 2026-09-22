package io.github.taetae98coding.calendar.lunar

import io.github.taetae98coding.calendar.Config
import io.github.taetae98coding.calendar.Paths
import io.github.taetae98coding.calendar.file.FileDataSource
import io.github.taetae98coding.calendar.openapi.kasi.KasiDataSource
import io.github.taetae98coding.calendar.openapi.kasi.KasiLunarItem
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.time.Clock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.todayIn

data object LunarUpdater {
    /** 한국천문연구원 음양력 정보의 시작 시점. 1391년 1월은 제공되지 않는다. */
    private val minYearMonth = YearMonth(Config.LUNAR_MIN_YEAR, 2)

    /**
     * 음력은 한 번 확정되면 바뀌지 않으므로 이미 만들어진 월 파일은 다시 호출하지 않는다.
     * 한 번의 실행에서 새로 호출할 월의 개수를 [Config.lunarFetchBudget] 으로 제한해,
     * 공공데이터포털 일일 트래픽 한도 안에서 여러 번의 스케줄 실행에 걸쳐 전체 범위를 채운다.
     */
    suspend fun update(config: Config): LunarResult {
        val budget = AtomicInteger(config.lunarFetchBudget)
        val today = Clock.System.todayIn(TimeZone.of("Asia/Seoul"))

        // 가까운 연도부터 채워 예산이 모자라도 실제로 많이 쓰이는 구간이 먼저 완성되게 한다.
        val years = config.lunarYears.sortedBy { year -> abs(year - today.year) }
        val completed = mutableSetOf<Int>()

        for (year in years) {
            if (updateYear(year, config, budget)) {
                completed += year
            }
        }

        val missing = config.lunarYears.filterNot(completed::contains)
        println("[Lunar] 완료 ${completed.size}년 / 미완료 ${missing.size}년, 잔여 예산=${budget.get()}")

        return LunarResult(completedYears = completed.sorted(), missingYears = missing)
    }

    private suspend fun updateYear(year: Int, config: Config, budget: AtomicInteger): Boolean {
        val yearMonths = (1..12).map { month -> YearMonth(year, month) }
            .filter { yearMonth -> yearMonth >= minYearMonth }

        val months = coroutineScope {
            yearMonths.map { yearMonth -> async { loadYearMonth(yearMonth, config, budget) } }
                .awaitAll()
        }

        if (months.any { it == null }) return false

        val lunarDates = months.filterNotNull().flatten()
        val file = Paths.lunarYear(year)

        if (config.fetchEnforce || !file.exists()) {
            FileDataSource.write(lunarDates, file)
        }

        return true
    }

    /** 이미 만들어진 파일이 있으면 재사용하고, 없으면 예산 안에서 새로 호출한다. 예산이 없으면 null. */
    private suspend fun loadYearMonth(yearMonth: YearMonth, config: Config, budget: AtomicInteger): List<LunarDate>? {
        val file = Paths.lunarYearMonth(yearMonth)

        if (!config.fetchEnforce) {
            FileDataSource.readOrNull<List<LunarDate>>(file)
                ?.takeIf(List<LunarDate>::isNotEmpty)
                ?.let { return it }
        }

        if (budget.getAndDecrement() <= 0) return null

        val lunarDates = runCatching { KasiDataSource.getLunar(yearMonth).map { item -> item.toLunarDate() } }
            .getOrElse { throwable ->
                println("[Lunar] $yearMonth 조회 실패: ${throwable.message}")
                return null
            }

        if (lunarDates.isEmpty()) return null

        return lunarDates.sortedBy(LunarDate::solar)
            .also { value -> FileDataSource.write(value, file) }
    }

    private fun KasiLunarItem.toLunarDate(): LunarDate {
        return LunarDate(
            solar = solarDate,
            year = lunarYearValue,
            month = lunarMonthValue,
            day = lunarDayValue,
            isLeapMonth = isLeapMonth,
        )
    }
}

data class LunarResult(
    val completedYears: List<Int>,
    val missingYears: List<Int>,
)
