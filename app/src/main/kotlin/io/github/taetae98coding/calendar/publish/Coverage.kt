package io.github.taetae98coding.calendar.publish

import io.github.taetae98coding.calendar.domain.holiday.Country
import io.github.taetae98coding.calendar.publish.DocApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 실제로 배포되어 있는 파일을 기준으로 제공 범위를 계산한다. */
data object Coverage {
    private val yearFileRegex = "^(\\d{4})\\.json$".toRegex()

    suspend fun years(api: DocApi, country: Country): List<Int> {
        return withContext(Dispatchers.IO) {
            DocPaths.country(api, country)
                .listFiles()
                .orEmpty()
                .mapNotNull { file -> yearFileRegex.find(file.name)?.groupValues?.get(1)?.toIntOrNull() }
                .sorted()
        }
    }
}

/** 연속된 연도를 구간으로 묶는다. 수십 개까지 늘어날 수 있는 목록을 짧게 표현하기 위함이다. */
fun List<Int>.toYearRanges(): List<YearRange> {
    return sorted().fold(emptyList()) { acc, year ->
        val last = acc.lastOrNull()

        if (last != null && last.endInclusive + 1 == year) {
            acc.dropLast(1) + last.copy(endInclusive = year)
        } else {
            acc + YearRange(year, year)
        }
    }
}
