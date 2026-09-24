plugins {
    id("calendar.kotlin-jvm")
    `java-test-fixtures`
}

/**
 * 계층과 무관한 바탕. 로그, JSON·텍스트 파일 입출력, 산출물 폴더 정리, 취소를 존중하는 실패 처리.
 *
 * 달력도, 원천도, 캐시도 모른다. 여기에 `project(...)` 가 생기거나 도메인 어휘가 들어오면 자리가 틀린 것이다.
 *
 * `testFixtures` 에는 모듈들이 테스트에서 함께 쓰는 도구(기록 로거, 고정 시계, 임시 폴더)가 있다.
 * 다른 모듈은 `testImplementation(testFixtures(project(":core")))` 로 가져다 쓴다.
 */
dependencies {
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
}
