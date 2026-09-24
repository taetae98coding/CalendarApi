plugins {
    id("calendar.kotlin-jvm")
}

/**
 * 원천에서 조회만 한다.
 *
 * 응답을 도메인 모델로 좁히지 않고 원본 그대로 돌려준다. 캐시·우선순위·재시도 정책을 모른다.
 * `:domain` 에도 의존하지 않는다. 출처의 표기를 도메인 어휘로 옮기는 일은 `:data` 의 몫이다.
 * `:core` 는 계층과 무관한 바탕이라 가져다 쓴다. (취소를 존중하는 실패 처리)
 */
dependencies {
    api(libs.kotlinx.serialization.json)
    api(ktorLibs.client.core)
    implementation(project(":core"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)

    implementation(ktorLibs.client.okhttp)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(ktorLibs.client.mock)
}
