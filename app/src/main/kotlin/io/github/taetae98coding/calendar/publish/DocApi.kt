package io.github.taetae98coding.calendar.publish

/**
 * 배포하는 API 세 가지. [id] 가 그대로 `docs/{id}/{country}/...` 의 첫 단계가 된다.
 *
 * 필요한 것만 받고 싶으면 [HOLIDAY] · [LUNAR] 를, 한 번에 받고 싶으면 [CALENDAR] 를 쓴다.
 */
enum class DocApi(val id: String) {
    HOLIDAY("holiday"),
    LUNAR("lunar"),
    CALENDAR("calendar"),
}
