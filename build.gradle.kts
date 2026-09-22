import java.time.LocalDate
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)

    implementation(ktorLibs.client.okhttp)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)

    testImplementation(kotlin("test"))
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
    description = "Collect holiday/lunar data and regenerate the static API under docs/"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.github.taetae98coding.calendar.AppKt")

    val today = LocalDate.now()

    val startYear = env("START_YEAR") ?: "1998"
    val endInclusiveYear = env("END_INCLUSIVE_YEAR") ?: (today.year + 3).toString()

    // 한국천문연구원 음양력 정보가 제공하는 전체 범위 (1391-02-05 ~ 2050-12-31)
    val lunarStartYear = env("LUNAR_START_YEAR") ?: "1391"
    val lunarEndInclusiveYear = env("LUNAR_END_INCLUSIVE_YEAR") ?: "2050"

    // data.go.kr 개발 계정의 일일 트래픽 한도를 넘지 않도록 한 번의 실행에서 새로 호출할 음력 월의 상한.
    // 이미 생성된 과거 월은 다시 호출하지 않으므로 여러 번의 스케줄 실행에 걸쳐 점진적으로 채워진다.
    val lunarFetchBudget = env("LUNAR_FETCH_BUDGET") ?: "3000"

    val fetchEnforce = env("FETCH_ENFORCE") ?: "false"
    val serviceKey = secret("SERVICE_KEY").orEmpty()

    environment("START_YEAR", startYear)
    environment("END_INCLUSIVE_YEAR", endInclusiveYear)
    environment("LUNAR_START_YEAR", lunarStartYear)
    environment("LUNAR_END_INCLUSIVE_YEAR", lunarEndInclusiveYear)
    environment("LUNAR_FETCH_BUDGET", lunarFetchBudget)
    environment("FETCH_ENFORCE", fetchEnforce)
    environment("SERVICE_KEY", serviceKey)
}
