import java.util.Properties

plugins {
    id("calendar.kotlin-jvm")
}

/** 설정을 읽고 계층을 조립해 한 번의 실행을 진행한다. 조립하는 자리라 모든 모듈에 의존한다. */
dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":datasource"))
    implementation(project(":data"))
    implementation(project(":publish"))
    implementation(libs.kotlinx.coroutines.core)

    // Ktor 가 SLF4J 로 로그를 보낸다. 구현이 없으면 실행마다 경고가 찍히므로 조용한 구현을 넣는다.
    runtimeOnly(libs.slf4j.nop)
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

    // 제공 범위(1998 ~ 2050)는 CalendarYears 에 고정되어 있다. 환경 변수로 바꾸지 않는다.
    // 한 번의 실행에서 다 받을 수는 없으므로, 서비스마다 예산만큼만 요청하고
    // 가장 오래 갱신되지 않은 구간부터 처리해 여러 번의 실행에 걸쳐 골고루 채운다.
    // 기본값은 Config 가 가진다. 여기서는 정해진 값만 넘긴다.
    listOf("FETCH_BUDGET", "MAX_CONCURRENCY", "MAX_REQUESTS_PER_SECOND").forEach { name ->
        env(name)?.let { value -> environment(name, value) }
    }
    secret("DATA_GO_KR_SERVICE_KEY")?.let { value -> environment("DATA_GO_KR_SERVICE_KEY", value) }
}
