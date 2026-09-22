/**
 * 모든 모듈이 같은 컴파일러·직렬화 플러그인·테스트·코드 스타일 설정을 쓴다.
 * 모듈 build.gradle.kts 에는 그 모듈만의 의존과 설명만 남긴다.
 *
 * 코드 스타일 규칙은 루트 .editorconfig 에 있고 `./gradlew ktlintCheck` 가 확인한다. `ktlintFormat` 으로 고칠 수 있다.
 */
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
}

dependencies {
    "testImplementation"(kotlin("test"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
