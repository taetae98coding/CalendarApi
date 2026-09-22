package io.github.taetae98coding.calendar

data class Config(
    val startYear: Int,
    val endInclusiveYear: Int,
    val lunarStartYear: Int,
    val lunarEndInclusiveYear: Int,
    val lunarFetchBudget: Int,
    val fetchEnforce: Boolean,
) {
    val years: IntRange
        get() = startYear..endInclusiveYear

    val lunarYears: IntRange
        get() = lunarStartYear..lunarEndInclusiveYear

    companion object {
        fun fromEnvironment(): Config {
            return Config(
                startYear = requireEnv("START_YEAR").toInt(),
                endInclusiveYear = requireEnv("END_INCLUSIVE_YEAR").toInt(),
                lunarStartYear = maxOf(requireEnv("LUNAR_START_YEAR").toInt(), LUNAR_MIN_YEAR),
                lunarEndInclusiveYear = minOf(requireEnv("LUNAR_END_INCLUSIVE_YEAR").toInt(), LUNAR_MAX_YEAR),
                lunarFetchBudget = requireEnv("LUNAR_FETCH_BUDGET").toInt(),
                fetchEnforce = System.getenv("FETCH_ENFORCE") == "true",
            )
        }

        private fun requireEnv(name: String): String {
            return requireNotNull(System.getenv(name)) { "환경 변수 $name 이(가) 필요합니다." }
        }

        /** 한국천문연구원 음양력 정보가 제공하는 최소 연도. (1391-02-05 부터) */
        const val LUNAR_MIN_YEAR = 1391

        /** 한국천문연구원 음양력 정보가 제공하는 최대 연도. (2050-12-31 까지) */
        const val LUNAR_MAX_YEAR = 2050
    }
}
