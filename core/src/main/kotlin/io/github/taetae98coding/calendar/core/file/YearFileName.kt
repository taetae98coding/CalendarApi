package io.github.taetae98coding.calendar.core.file

/**
 * 캐시와 배포 문서가 공유하는 연도 항목 이름. 연 단위 파일(`2026.json`)과 월 폴더(`2026`)가 같은 자리에 놓인다.
 */
object YearFileName {
    private val regex = "^(\\d{4})(\\.json)?$".toRegex()

    /** 이름이 연도 파일이나 연도 폴더면 그 연도, 아니면 null. */
    fun yearOf(name: String): Int? = regex.find(name)?.groupValues?.get(1)?.toIntOrNull()

    fun file(year: Int): String = "$year.json"

    fun directory(year: Int): String = "$year"
}
