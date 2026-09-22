plugins {
    id("calendar.kotlin-jvm")
}

/**
 * 캐시를 관리하고 원본을 도메인으로 옮긴다.
 *
 * - `cache` : 응답 원본과 갱신 기록을 `cache/` 에 읽고 쓴다.
 * - `update` : 예산·순서·동시성을 관리해 캐시를 갱신한다.
 * - `source` : 캐시 폴더 하나에 대응하는 원천 API 를 식별하고 실제 원천을 부른다.
 * - `holiday` · `lunar` : 캐시만 읽어 `:domain` 의 저장소 인터페이스를 구현한다.
 */
dependencies {
    api(project(":domain"))
    api(project(":datasource"))
    implementation(project(":core"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
}
