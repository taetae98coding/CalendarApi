package io.github.taetae98coding.calendar.holiday

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Holiday(
    @SerialName("name")
    val name: String,
    @SerialName("isHoliday")
    val isHoliday: Boolean,
    @SerialName("start")
    val start: LocalDate,
    @SerialName("endInclusive")
    val endInclusive: LocalDate,
)

/** 같은 이름으로 연속된 날짜를 하나의 기간으로 합친다. (예: 설날 3일) */
fun List<Holiday>.holidayFold(): List<Holiday> {
    return groupBy(Holiday::name).values
        .map { list -> list.sortedBy(Holiday::start) }
        .map { list ->
            list.fold(emptyList<Holiday>()) { acc, holiday ->
                if (acc.isEmpty()) {
                    return@fold listOf(holiday)
                }

                val last = acc.last()
                if (last.endInclusive.plus(1, DateTimeUnit.DAY) == holiday.start) {
                    acc.dropLast(1) + last.copy(endInclusive = holiday.endInclusive)
                } else {
                    acc + holiday
                }
            }
        }
        .flatten()
}

fun List<Holiday>.holidaySorted(): List<Holiday> {
    return sortedWith { a, b ->
        if (a.start != b.start) return@sortedWith compareValues(a.start, b.start)
        if (a.name != b.name) return@sortedWith compareValues(a.name, b.name)

        0
    }
}
