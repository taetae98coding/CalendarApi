package io.github.taetae98coding.calendar.domain.holiday

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

/**
 * 여러 출처에서 모은 공휴일을 배포 가능한 하나의 목록으로 정리한다.
 *
 * 순서가 중요하다. 같은 날을 먼저 합쳐야 연속 기간을 올바르게 이을 수 있다.
 */
fun List<Holiday>.normalized(): List<Holiday> = distinctByDate().foldConsecutive().sortedByStart()

/**
 * 같은 이름·같은 날짜를 하나로 합친다.
 *
 * 특일 정보는 같은 날을 여러 API 가 내려주는데 isHoliday 가 서로 다른 경우가 있다.
 * (예: 어린이날은 getRestDeInfo 에서 Y, getAnniversaryInfo 에서 N)
 * 동시 호출이라 도착 순서가 매번 달라지므로, 먼저 온 것을 고르면 실행할 때마다 결과가 바뀐다.
 * 휴일 쪽으로 합쳐 결과를 고정한다.
 */
fun List<Holiday>.distinctByDate(): List<Holiday> {
    return groupBy { holiday -> holiday.name to holiday.start }
        .map { (_, holidays) -> holidays.first().copy(isHoliday = holidays.any(Holiday::isHoliday)) }
}

/** 같은 이름으로 연속된 날짜를 하나의 기간으로 합친다. (예: 설날 3일) */
fun List<Holiday>.foldConsecutive(): List<Holiday> {
    return groupBy(Holiday::name).values.flatMap { holidays ->
        holidays.sortedBy(Holiday::start).fold(mutableListOf()) { acc, holiday ->
            val last = acc.lastOrNull()

            if (last != null && last.endInclusive.plus(1, DateTimeUnit.DAY) == holiday.start) {
                acc[acc.lastIndex] = last.copy(endInclusive = holiday.endInclusive)
            } else {
                acc += holiday
            }

            acc
        }
    }
}

fun List<Holiday>.sortedByStart(): List<Holiday> = sortedWith(compareBy(Holiday::start, Holiday::name))
