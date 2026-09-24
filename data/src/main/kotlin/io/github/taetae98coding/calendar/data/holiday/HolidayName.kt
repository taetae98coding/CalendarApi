package io.github.taetae98coding.calendar.data.holiday

/**
 * 같은 공휴일이 출처와 연도에 따라 다른 이름으로 내려온다.
 * (KASI: 석가탄신일 / 부처님오신날, Nager: 새해 / 3·1절, 연도별: 4·19 혁명 기념일 / 4·19혁명 기념일)
 * 클라이언트가 연도 경계에서 다른 이름을 보지 않도록 한 가지로 맞춘다.
 *
 * 같은 이름인지는 공백을 모두 지운 표기로 판단하고, 내보내는 이름은 한글 맞춤법의 띄어쓰기 원칙(단어별)을 따른다.
 * 법령이 명칭을 바꾼 공휴일·기념일은 예전 연도까지 바뀐 명칭을 법령 표기 그대로 쓴다. ([renamed])
 *
 * 출처별 표기의 차이를 아는 일이라 `:domain` 이 아니라 여기, 공통 매퍼([HolidayMapper]) 옆에 둔다.
 * 도메인은 정리된 이름만 본다.
 */
object HolidayName {
    private val whitespace = "\\s+".toRegex()
    private val ordinalPrefix = "^제\\s*\\d+\\s*대\\s*".toRegex()

    /** 이 표기로 시작하면 무엇을 대신하는지까지 출처가 적어 주므로 손대지 않는다. (대체공휴일(설날), 임시공휴일(제21대 대통령 선거)) */
    private val providedAsIs = listOf("임시공휴일", "대체공휴일", "대체휴무일")

    /** 명절은 연휴의 앞뒤 날을 따로 부르지 않고 명절 이름 하나로 모은다. */
    private val festivals = listOf(
        "^(설날|설|구정)(연휴|전날|다음날|당일)?$".toRegex() to "설날",
        "^(추석|한가위)(연휴|전날|다음날|당일)?$".toRegex() to "추석",
    )

    /**
     * 법령(관공서의 공휴일에 관한 규정, 각종 기념일 등에 관한 규정 등)이 명칭을 바꾼 경우. 예전 명칭 → 바뀐 명칭.
     * 바뀐 명칭은 법령 표기 그대로 쓰고 맞춤법 띄어쓰기를 적용하지 않는다. 바뀐 명칭의 띄어쓰기 변형도 여기서 모은다.
     */
    private val renamed = listOf(
        listOf("석가탄신일", "부처님오신날") to "부처님오신날",
        listOf("근로자의 날", "노동절") to "노동절",
        listOf("체육의 날", "스포츠의 날") to "스포츠의 날",
        listOf("저축의 날", "금융의 날") to "금융의 날",
        listOf("향토예비군의 날", "예비군의 날") to "예비군의 날",
        listOf("소비자 보호의 날", "소비자의 날") to "소비자의 날",
        listOf("원자력의 날", "원자력 안전 및 진흥의 날") to "원자력 안전 및 진흥의 날",
        listOf("지방자치의 날", "지방자치 및 균형발전의 날") to "지방자치 및 균형발전의 날",
        listOf("학생의 날", "학생독립운동 기념일") to "학생독립운동 기념일",
        listOf("임시정부수립기념일", "대한민국 임시정부 수립 기념일") to "대한민국 임시정부 수립 기념일",
    ).flatMap { (names, current) -> names.map { name -> compact(name) to current } }.toMap()

    /** 맞춤법에 맞는 표기. 공백만 다른 표기는 모두 이 표기로 바뀐다. */
    private val spellings = listOf(
        "어린이날",
        "어버이날",
        "부부의 날",
        "환경의 날",
        "세계 한인의 날",
        "아동 학대 예방의 날",
        "3·8 민주 의거 기념일",
        "3·15 의거 기념일",
        "4·3 희생자 추념일",
        "4·19 혁명 기념일",
        "5·18 민주화 운동 기념일",
        "6·10 만세 운동 기념일",
        "6·10 민주 항쟁 기념일",
        "6·25 전쟁일",
        "국회의원 선거",
        "대통령 선거",
        "전국 동시 지방 선거",
    ).associateBy(::compact)

    /** 표기 자체가 다른 이름 → 맞춤법에 맞는 표기. 키도 공백을 지우고 비교한다. */
    private val aliases = mapOf(
        "1월1일" to "신정",
        "새해" to "신정",
        "3·1절" to "삼일절",
        "기독탄신일" to "크리스마스",
        "어버이의 날" to "어버이날",
        "국회의원 선거일" to "국회의원 선거",
        "대통령 선거일" to "대통령 선거",
        "동시 지방 선거일" to "전국 동시 지방 선거",
        "지방 선거일" to "전국 동시 지방 선거",
    ).mapKeys { (alias, _) -> compact(alias) }

    fun normalize(raw: String): String {
        val trimmed = raw.trim().replace(whitespace, " ")
        if (providedAsIs.any { prefix -> compact(trimmed).startsWith(prefix) }) return raw

        // 제21대 대통령 선거 -> 대통령 선거
        val withoutOrdinal = trimmed.replace(ordinalPrefix, "")
        val key = compact(withoutOrdinal)

        festivals.firstOrNull { (pattern, _) -> pattern.matches(key) }?.let { (_, name) -> return name }

        return renamed[key] ?: aliases[key] ?: spellings[key] ?: withoutOrdinal
    }

    private fun compact(name: String): String = name.replace(whitespace, "")
}
