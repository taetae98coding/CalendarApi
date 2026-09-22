package io.github.taetae98coding.calendar.lunar

import io.github.taetae98coding.calendar.Config
import io.github.taetae98coding.calendar.Paths
import io.github.taetae98coding.calendar.file.FileDataSource
import io.github.taetae98coding.calendar.openapi.entity.OpenApiException
import io.github.taetae98coding.calendar.openapi.kasi.KasiDataSource
import io.github.taetae98coding.calendar.openapi.kasi.KasiLunarItem
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.time.Clock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.todayIn
import kotlinx.datetime.yearMonth
import kotlinx.serialization.json.JsonElement

data object LunarUpdater {
    /** 동시에 처리할 연도 수. 월 단위 호출은 이 안에서 다시 12개씩 동시에 나간다. */
    private const val YEAR_PARALLELISM = 4

    private val rateLimitLogged = AtomicBoolean(false)

    /** 한국천문연구원 음양력 정보의 시작 시점. 1391년 1월은 제공되지 않는다. */
    private val minYearMonth = YearMonth(Config.LUNAR_MIN_YEAR, 2)

    /**
     * 음력은 한 번 확정되면 바뀌지 않으므로 이미 만들어진 월 파일은 다시 호출하지 않는다.
     * 한 번의 실행에서 새로 호출할 월의 개수를 [Config.lunarFetchBudget] 으로 제한해,
     * 공공데이터포털 일일 트래픽 한도 안에서 여러 번의 스케줄 실행에 걸쳐 전체 범위를 채운다.
     */
    suspend fun update(config: Config, isRegistered: Boolean): LunarResult {
        if (!isRegistered) {
            println("[Lunar] 음양력 정보가 활용신청되지 않아 건너뜁니다.")

            return LunarResult(completedYears = emptyList(), missingYears = config.lunarYears.toList())
        }

        val budget = AtomicInteger(config.lunarFetchBudget)
        val quotaExceeded = AtomicBoolean(false)
        val today = Clock.System.todayIn(TimeZone.of("Asia/Seoul"))

        // 가까운 연도부터 채워 예산이 모자라도 실제로 많이 쓰이는 구간이 먼저 완성되게 한다.
        // Semaphore 는 FIFO 라 연도를 한꺼번에 띄워도 이 순서가 대체로 유지된다.
        val years = config.lunarYears.sortedBy { year -> abs(year - today.year) }
        val yearSemaphore = Semaphore(YEAR_PARALLELISM)

        val completed = coroutineScope {
            years.map { year -> async { year to yearSemaphore.withPermit { updateYear(year, config, budget, quotaExceeded) } } }
                .awaitAll()
        }
            .filter { (_, isCompleted) -> isCompleted }
            .map { (year, _) -> year }

        val completedYears = completed.toSet()
        val missing = config.lunarYears.filterNot(completedYears::contains)
        println("[Lunar] 완료 ${completedYears.size}년 / 미완료 ${missing.size}년, 잔여 예산=${budget.get().coerceAtLeast(0)}")

        return LunarResult(completedYears = completed.sorted(), missingYears = missing)
    }

    /**
     * 한국천문연구원은 그레고리력 도입 이전 구간을 율리우스력 기준의 월로 내려준다.
     * 그래서 요청한 월과 실제 양력 날짜가 최대 열흘까지 어긋난다. (1500-02 요청 -> 1500-02-10 ~ 1500-03-10)
     * 앞뒤 달을 함께 읽어 실제 양력 날짜로 다시 묶어야 월 파일이 이름과 맞는다.
     */
    private suspend fun updateYear(year: Int, config: Config, budget: AtomicInteger, quotaExceeded: AtomicBoolean): Boolean {
        val ownYearMonths = (1..12).map { month -> YearMonth(year, month) }.filter(::isAvailable)
        val neighbourYearMonths = listOf(YearMonth(year - 1, 12), YearMonth(year + 1, 1)).filter(::isAvailable)

        val own = coroutineScope {
            ownYearMonths.map { yearMonth -> async { loadYearMonth(yearMonth, config, budget, quotaExceeded) } }
                .awaitAll()
        }

        if (own.any { it == null }) return false

        val neighbours = coroutineScope {
            neighbourYearMonths.map { yearMonth -> async { loadYearMonth(yearMonth, config, budget, quotaExceeded) } }
                .awaitAll()
        }

        val lunarDates = (own + neighbours).filterNotNull()
            .flatten()
            .sortedWith(compareBy(LunarDate::solar, LunarDate::year, LunarDate::month, LunarDate::day))
            // 1582년 그레고리력 개혁으로 같은 양력 날짜가 두 번 나오는 구간이 있다.
            .distinctBy(LunarDate::solar)
            .filter { lunarDate -> lunarDate.solar.year == year }

        if (lunarDates.isEmpty()) return false

        coroutineScope {
            launch { FileDataSource.write(lunarDates, Paths.lunarYear(year)) }

            lunarDates.groupBy { lunarDate -> lunarDate.solar.yearMonth }
                .forEach { (yearMonth, dates) -> launch { FileDataSource.write(dates, Paths.lunarYearMonth(yearMonth)) } }
        }

        return true
    }

    private fun isAvailable(yearMonth: YearMonth): Boolean {
        return yearMonth >= minYearMonth && yearMonth.year <= Config.LUNAR_MAX_YEAR
    }

    /** 캐시에 원본이 있으면 재사용하고, 없으면 예산 안에서 새로 호출한다. 예산이 없거나 실패하면 null. */
    private suspend fun loadYearMonth(yearMonth: YearMonth, config: Config, budget: AtomicInteger, quotaExceeded: AtomicBoolean): List<LunarDate>? {
        val file = Paths.kasiLunarCache(yearMonth)
        val description = "KASI 음양력 $yearMonth"

        val cached = if (!config.fetchEnforce && file.exists()) {
            FileDataSource.readOrNull<JsonElement>(file)
        } else {
            null
        }

        val raw = cached ?: run {
            if (quotaExceeded.get() || budget.get() <= 0) return null

            runCatching { KasiDataSource.getLunar(yearMonth) }
                .onSuccess { value ->
                    budget.decrementAndGet()
                    FileDataSource.writeCache(value, file)
                }
                .getOrElse { throwable ->
                    onFetchFailure(description, throwable, budget, quotaExceeded)

                    return null
                }
        }

        val lunarDates = runCatching {
            KasiDataSource.parseItems<KasiLunarItem>(raw, description)
                .map { item -> item.toLunarDate() }
                .sortedBy(LunarDate::solar)
        }
            .getOrElse { throwable ->
                println("[Lunar] $yearMonth 해석 실패: ${throwable.message}")

                return null
            }

        return lunarDates.ifEmpty { null }
    }

    /**
     * 실패 원인에 따라 예산 처리가 다르다.
     * 속도 초과는 호출이 성립하지 않은 것이므로 예산을 깎지 않고, 일일 한도를 다 쓴 경우에는 남은 호출을 멈춘다.
     * 수천 건이 한꺼번에 실패할 수 있어 같은 사유는 한 번만 출력한다.
     */
    private fun onFetchFailure(description: String, throwable: Throwable, budget: AtomicInteger, quotaExceeded: AtomicBoolean) {
        val openApiException = throwable as? OpenApiException

        when {
            openApiException?.isQuotaExceeded == true -> {
                if (quotaExceeded.compareAndSet(false, true)) {
                    println("[Lunar] 일일 트래픽을 모두 사용했습니다. 남은 연도는 다음 실행에서 이어서 채웁니다.")
                }
            }

            openApiException?.isRateLimited == true -> {
                if (rateLimitLogged.compareAndSet(false, true)) {
                    println("[Lunar] 요청 속도 초과로 일부 월을 건너뜁니다. MAX_REQUESTS_PER_SECOND 를 낮춰 보세요.")
                }
            }

            else -> {
                budget.decrementAndGet()
                println("[Lunar] $description 조회 실패: ${throwable.message}")
            }
        }
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
