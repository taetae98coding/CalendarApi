package io.github.taetae98coding.calendar.domain.holiday

/**
 * 같은 공휴일이 출처와 연도에 따라 다른 이름으로 내려온다.
 * (KASI: 석가탄신일 / 부처님오신날, Nager: 새해 / 3·1절)
 * 클라이언트가 연도 경계에서 다른 이름을 보지 않도록 한 가지로 맞춘다.
 */
object HolidayName {
    private val whitespace = "\\s+".toRegex()
    private val ordinalPrefix = "^제\\s*\\d+\\s*대\\s*".toRegex()
    private val temporary = "^임시공휴일\\((.+)\\)$".toRegex()

    /** 공백을 지운 표기가 이 이름들과 같으면 그 이름으로 고정한다. 공백만 다른 표기가 여기서 흡수된다. */
    private val canonical = setOf(
        "부처님오신날",
        "어린이날",
        "어버이날",
        "국회의원선거",
        "대통령선거",
        "전국동시지방선거",
    )

    /** 공백을 지운 표기 → 고정 이름. */
    private val aliases = mapOf(
        "1월1일" to "신정",
        "새해" to "신정",
        "3·1절" to "삼일절",
        "기독탄신일" to "크리스마스",
        "석가탄신일" to "부처님오신날",
        "어버이의날" to "어버이날",
        "국회의원선거일" to "국회의원선거",
        "대통령선거일" to "대통령선거",
        "동시지방선거일" to "전국동시지방선거",
        "대체휴무일" to "대체공휴일",
    )

    fun normalize(raw: String): String {
        val trimmed = raw.trim().replace(whitespace, " ")

        // 임시공휴일(국군의 날) 처럼 대상이 괄호에 담겨 오면 대상 이름으로 본다.
        temporary.find(trimmed)?.let { match -> return normalize(match.groupValues[1]) }

        // 제21대 대통령 선거 -> 대통령선거
        val withoutOrdinal = trimmed.replace(ordinalPrefix, "")
        val compact = withoutOrdinal.replace(" ", "")

        return when {
            compact in canonical -> compact
            else -> aliases[compact] ?: withoutOrdinal
        }
    }
}
