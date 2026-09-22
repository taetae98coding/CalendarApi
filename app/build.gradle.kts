import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

/** 실행 진입점과 배포 문서 생성. 계층을 조립하는 자리라 셋 모두에 의존한다. */
dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":datasource"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}

/** GitHub Actions 는 값이 없는 input 을 빈 문자열로 넘기므로 공백도 미지정으로 본다. */
fun env(name: String): String? = System.getenv(name)?.takeIf(String::isNotBlank)

/**
 * CI 는 환경 변수(저장소 시크릿)로, 로컬은 secrets.properties 로 인증키를 넘긴다.
 * secrets.properties 는 .gitignore 대상이라 커밋되지 않는다. 템플릿은 secrets.properties.example 참고.
 */
val secretsProperties = Properties().apply {
    rootProject.file("secrets.properties")
        .takeIf(File::exists)
        ?.inputStream()
        ?.use(::load)
}

fun secret(name: String): String? = (env(name) ?: secretsProperties.getProperty(name))?.takeIf(String::isNotBlank)

tasks.register<JavaExec>("updateCalendar") {
    group = "calendar"
    description = "Refresh the response cache and regenerate the static API under docs/"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.github.taetae98coding.calendar.AppKt")

    // cache/ 와 docs/ 는 저장소 루트 기준의 상대 경로다. 모듈 디렉터리에서 실행되지 않도록 고정한다.
    workingDir = rootProject.projectDir

    // 제공 범위(1998 ~ 2050)는 Config 에 고정되어 있다. 환경 변수로 바꾸지 않는다.
    // 한 번의 실행에서 다 받을 수는 없으므로, 서비스마다 이 개수만큼만 요청하고
    // 가장 오래 갱신되지 않은 폴더부터 처리해 여러 번의 실행에 걸쳐 골고루 채운다.
    val fetchBudget = env("FETCH_BUDGET") ?: "3000"

    val maxConcurrency = env("MAX_CONCURRENCY") ?: "8"
    val maxRequestsPerSecond = env("MAX_REQUESTS_PER_SECOND") ?: "20"
    val serviceKey = secret("DATA_GO_KR_SERVICE_KEY").orEmpty()

    environment("FETCH_BUDGET", fetchBudget)
    environment("MAX_CONCURRENCY", maxConcurrency)
    environment("MAX_REQUESTS_PER_SECOND", maxRequestsPerSecond)
    environment("DATA_GO_KR_SERVICE_KEY", serviceKey)
}
