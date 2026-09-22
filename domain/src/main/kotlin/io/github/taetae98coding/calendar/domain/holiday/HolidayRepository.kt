package io.github.taetae98coding.calendar.domain.holiday

/**
 * 공휴일을 읽는다.
 *
 * 어느 원천에서 왔는지, 캐시가 있는지, 이번 실행에서 무엇을 새로 받았는지는 여기에 없다.
 * 배포 문서를 만드는 쪽은 이 인터페이스만 보면 된다.
 */
interface HolidayRepository {
    suspend fun get(country: Country, year: Int): List<Holiday>
}
