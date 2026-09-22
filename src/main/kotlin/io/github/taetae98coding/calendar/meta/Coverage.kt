package io.github.taetae98coding.calendar.meta

import io.github.taetae98coding.calendar.Paths
import io.github.taetae98coding.calendar.holiday.Country
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * 실제로 배포되어 있는 파일을 기준으로 제공 범위를 계산한다.
 *
 * 범위를 좁혀 수동 실행하더라도 meta.json 과 index.html 이 그 범위로 줄어들지 않게 하기 위함이다.
 */
data object Coverage {
    private val yearFileRegex = "^(\\d{4})\\.json$".toRegex()

    suspend fun holidayYears(): Map<Country, List<Int>> {
        return coroutineScope {
            Country.entries.map { country -> async { country to years(Paths.holidayDirectory(country)) } }
                .awaitAll()
                .toMap()
        }
    }

    suspend fun lunarYears(): List<Int> = years(Paths.lunarDirectory)

    private suspend fun years(directory: File): List<Int> {
        return withContext(Dispatchers.IO) {
            directory.listFiles()
                .orEmpty()
                .mapNotNull { file -> yearFileRegex.find(file.name)?.groupValues?.get(1)?.toIntOrNull() }
                .sorted()
        }
    }
}

/** 연속된 연도를 구간으로 묶는다. 660개까지 늘어날 수 있는 목록을 짧게 표현하기 위함이다. */
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
