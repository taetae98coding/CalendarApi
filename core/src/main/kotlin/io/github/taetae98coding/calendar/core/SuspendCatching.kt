package io.github.taetae98coding.calendar.core

import kotlin.coroutines.cancellation.CancellationException

/**
 * [runCatching] 과 같지만 취소는 잡지 않는다.
 *
 * 코루틴 안에서 `runCatching` 을 쓰면 [CancellationException] 까지 [Result.failure] 로 삼켜져,
 * 바깥이 멈추라고 한 것이 "구간 하나의 실패" 로 둔갑한다. 형제 코루틴 하나가 죽어 나머지가 취소될 때
 * 취소된 요청 수만큼 실패 로그가 찍히고 연속 실패로 세어지는 것이 그 결과다.
 * 중단 가능한 자리에서 실패를 값으로 다루려면 이것을 쓴다.
 */
inline fun <T> runSuspendCatching(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (exception: CancellationException) {
        throw exception
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }
}
