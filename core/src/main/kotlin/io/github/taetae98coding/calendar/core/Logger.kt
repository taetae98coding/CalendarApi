package io.github.taetae98coding.calendar.core

/**
 * 실행 중 사람에게 알릴 한 줄.
 *
 * 수집은 GitHub Actions 로그가 유일한 관측 수단이라 형식보다 내용이 중요하다.
 * 테스트에서는 모아 두고 확인한다.
 */
fun interface Logger {
    fun log(message: String)

    companion object {
        val Stdout: Logger = Logger(::println)
    }
}
