plugins {
    id("calendar.kotlin-jvm")
}

/**
 * 캐시를 관리하고 원본을 도메인으로 옮긴다.
 *
 * - `cache` : 응답 원본과 갱신 기록을 `cache/` 에 읽고 쓴다.
 * - `update` : 순서·동시성·중단을 관리해 캐시를 갱신한다.
 * - `source` : 캐시 폴더 하나에 대응하는 원천 API 를 식별하고 실제 원천을 부른다.
 * - `holiday` · `lunar` : 캐시만 읽어 `:domain` 의 저장소 인터페이스를 구현한다. 출처별 표기를 도메인 어휘로 맞추는 매퍼도 여기 있다.
 *
 * `:core` 의 Logger 가 생성자에 드러나므로 `api` 로 노출한다.
 */
dependencies {
    api(project(":core"))
    api(project(":domain"))
    api(project(":datasource"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(testFixtures(project(":core")))
}
