package io.github.taetae98coding.calendar

import java.io.File

/**
 * 실행 한 번의 설정. 환경 변수를 읽는 유일한 자리다.
 *
 * 제공 범위는 여기 없다. 환경에 따라 달라지면 안 되는 값이라
 * [io.github.taetae98coding.calendar.domain.CalendarYears] 에 고정되어 있다.
 *
 * 한 번의 실행에서 몇 건을 받을지도 여기 없다. 원천이 일일 한도에 닿았다고 알려줄 때까지 받으므로
 * 우리가 정할 값이 아니다. [io.github.taetae98coding.calendar.data.update.ServiceGate] 를 보라.
 */
data class Config(
    /** 공공데이터포털 일반 인증키(Decoding). */
    val serviceKey: String,
    /** 외부 API 한 곳으로 동시에 나갈 수 있는 요청 수. */
    val maxConcurrency: Int = DEFAULT_MAX_CONCURRENCY,
    /** 외부 API 한 곳으로 나가는 초당 요청 수. */
    val maxRequestsPerSecond: Int = DEFAULT_MAX_REQUESTS_PER_SECOND,
    /** 응답 원본. 저장소 루트 기준이다. */
    val cacheRoot: File = File("cache"),
    /** 배포 문서. GitHub Pages 가 그대로 서빙한다. */
    val docsRoot: File = File("docs"),
) {
    companion object {
        const val DEFAULT_MAX_CONCURRENCY = 8
        const val DEFAULT_MAX_REQUESTS_PER_SECOND = 20

        fun fromEnvironment(): Config {
            val serviceKey = requireNotNull(env("DATA_GO_KR_SERVICE_KEY")) {
                "DATA_GO_KR_SERVICE_KEY(공공데이터포털 일반 인증키 Decoding)가 없습니다. " +
                    "secrets.properties.example 을 secrets.properties 로 복사해 채우거나 DATA_GO_KR_SERVICE_KEY 환경 변수를 설정하세요."
            }

            return Config(
                serviceKey = serviceKey,
                maxConcurrency = env("MAX_CONCURRENCY")?.toIntOrNull() ?: DEFAULT_MAX_CONCURRENCY,
                maxRequestsPerSecond = env("MAX_REQUESTS_PER_SECOND")?.toIntOrNull() ?: DEFAULT_MAX_REQUESTS_PER_SECOND,
            )
        }

        /** GitHub Actions 는 값이 없는 input 을 빈 문자열로 넘기므로 공백도 미지정으로 본다. */
        private fun env(name: String): String? = System.getenv(name)?.takeIf(String::isNotBlank)
    }
}
