plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

/**
 * 달력 자체의 개념만 둔다.
 *
 * 어디서 받아오는지(KASI·Nager), 어떻게 저장하는지(캐시·파일)를 모른다.
 * 프로젝트 의존이 없다는 것이 이 모듈의 정의다. 여기에 `project(...)` 가 생기면 계층이 무너진 것이다.
 */
dependencies {
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
