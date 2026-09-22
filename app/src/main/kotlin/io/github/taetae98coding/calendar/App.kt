package io.github.taetae98coding.calendar

suspend fun main() {
    CalendarApp(Config.fromEnvironment()).run()
}
