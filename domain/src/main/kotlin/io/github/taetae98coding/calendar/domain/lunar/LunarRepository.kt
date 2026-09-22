package io.github.taetae98coding.calendar.domain.lunar

/** 음력을 읽는다. 음력은 국가와 무관하게 같은 자료라 국가를 받지 않는다. */
interface LunarRepository {
    suspend fun get(year: Int): List<LunarDate>
}
