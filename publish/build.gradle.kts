plugins {
    id("calendar.kotlin-jvm")
}

/**
 * 저장소에서 읽은 도메인 모델을 배포 문서(docs/)로 쓴다.
 *
 * 입력은 `:domain` 의 저장소 인터페이스만이다. 자료가 어느 원천에서 왔는지, 캐시가 어떻게 생겼는지 모른다.
 * 여기에 `:data` 나 `:datasource` 의존이 생기면 배포 규격이 수집 사정에 끌려가기 시작한 것이다.
 */
dependencies {
    api(project(":domain"))
    implementation(project(":core"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
}
