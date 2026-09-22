plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

/**
 * 캐시를 관리하고 원본을 도메인으로 옮긴다.
 *
 * - 조회 성공/실패에 따라 캐시를 쓸지 말지 정한다.
 * - 캐시에 남은 마지막 갱신 시각으로 갱신 우선순위를 정한다.
 * - 예산과 동시성을 관리한다.
 */
dependencies {
    api(project(":domain"))
    implementation(project(":datasource"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}
