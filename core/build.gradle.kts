plugins {
    id("calendar.kotlin-jvm")
}

/**
 * 계층과 무관한 바탕. 로그, JSON 파일 입출력, 산출물 폴더 정리.
 *
 * 달력도, 원천도, 캐시도 모른다. 여기에 `project(...)` 가 생기거나 도메인 어휘가 들어오면 자리가 틀린 것이다.
 */
dependencies {
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
}
