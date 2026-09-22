package io.github.taetae98coding.calendar

data class Config(
    /** 한 번의 실행에서 서비스 하나에 보낼 수 있는 최대 요청 수. */
    val fetchBudget: Int,
) {
    companion object {
        /**
         * 모든 API 가 제공하는 고정 범위.
         *
         * 원천 API 가 실제로 제공하는 범위는 서비스마다 다르고 언제 바뀔지 모른다.
         * 제공 여부와 무관하게 이 범위로 고정해 두고, 없는 구간은 비어 있는 채로 둔다.
         * 이 범위 밖의 캐시와 배포 문서는 [io.github.taetae98coding.calendar.Pruner] 가 지운다.
         */
        const val START_YEAR = 1998

        /** @see START_YEAR */
        const val END_INCLUSIVE_YEAR = 2050

        val years: IntRange = START_YEAR..END_INCLUSIVE_YEAR

        private const val DEFAULT_FETCH_BUDGET = 3000

        fun fromEnvironment(): Config {
            return Config(
                fetchBudget = System.getenv("FETCH_BUDGET")?.toIntOrNull() ?: DEFAULT_FETCH_BUDGET,
            )
        }
    }
}
