plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.plugin.serialization) apply false
}

/**
 * 계층은 Gradle 모듈로 강제한다. 의존 방향은 아래 한 줄로 고정이고, 거꾸로 부르면 컴파일이 실패한다.
 *
 * `:domain` <- `:data` -> `:datasource`, 그리고 `:app` 이 셋을 조립한다.
 *
 * - `:domain` : 달력 자체의 개념. 다른 모듈을 모른다.
 * - `:datasource` : 원천에서 조회만 한다. 응답 원본을 그대로 돌려주고 도메인을 모른다.
 * - `:data` : 캐시를 성공/실패로 관리하고 갱신 우선순위와 동시성을 맡는다. 원본을 도메인으로 옮긴다.
 * - `:app` : 실행 진입점과 배포 문서 생성.
 */
subprojects {
    repositories {
        mavenCentral()
    }
}
