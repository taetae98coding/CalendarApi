package io.github.taetae98coding.calendar

/**
 * 실행 한 번의 설정. 제공 범위는 여기 없다.
 *
 * 범위는 환경에 따라 달라지면 안 되는 값이라 [io.github.taetae98coding.calendar.domain.CalendarYears] 에 고정되어 있다.
 */
data class Config(
    /** 한 번의 실행에서 서비스 하나에 보낼 수 있는 최대 요청 수. */
    val fetchBudget: Int,
) {
    companion object {
        private const val DEFAULT_FETCH_BUDGET = 3000

        fun fromEnvironment(): Config {
            return Config(
                fetchBudget = System.getenv("FETCH_BUDGET")?.toIntOrNull() ?: DEFAULT_FETCH_BUDGET,
            )
        }
    }
}
