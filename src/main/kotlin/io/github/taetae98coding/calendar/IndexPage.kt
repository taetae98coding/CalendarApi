package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.file.FileDataSource
import io.github.taetae98coding.calendar.meta.ApiMeta
import io.github.taetae98coding.calendar.meta.YearRange
import java.io.File

/** GitHub Pages 루트에 놓일 간단한 문서 페이지를 생성한다. */
data object IndexPage {
    suspend fun write(meta: ApiMeta) {
        FileDataSource.writeText("", File(Paths.docs, ".nojekyll"))
        FileDataSource.writeText(html(meta), File(Paths.docs, "index.html"))
    }

    private fun html(meta: ApiMeta): String {
        val holidayRange = range(meta.holiday.startYear, meta.holiday.endInclusiveYear)
        val lunarRange = meta.lunar.generated.joinToString(", ") { range -> range.text() }
            .ifBlank { "없음" }

        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>CalendarApi</title>
                <style>
                    :root { color-scheme: light dark; }
                    body { margin: 0 auto; padding: 32px 20px 64px; max-width: 760px; line-height: 1.7; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Apple SD Gothic Neo", "Noto Sans KR", sans-serif; }
                    code { padding: 2px 6px; border-radius: 4px; background: rgba(127, 127, 127, 0.18); font-size: 0.92em; }
                    h2 { margin-top: 40px; }
                    table { width: 100%; border-collapse: collapse; }
                    th, td { padding: 8px 6px; border-bottom: 1px solid rgba(127, 127, 127, 0.3); text-align: left; vertical-align: top; }
                    .updated { color: rgba(127, 127, 127, 1); font-size: 0.9em; }
                </style>
            </head>
            <body>
            <h1>CalendarApi</h1>
            <p>공휴일과 음력 정보를 정적 JSON 으로 제공하는 API 입니다. GitHub Actions 가 주기적으로 갱신합니다.</p>
            <p class="updated">마지막 갱신 : ${meta.updatedAt}</p>

            <h2>공휴일</h2>
            <p>제공 범위 : <code>$holidayRange</code></p>
            <table>
                <tr><th>국가</th><th>코드</th><th>범위</th></tr>
                ${meta.holiday.countries.joinToString("\n                ") { country ->
                    "<tr><td>${country.name}</td><td><code>${country.code}</code></td><td>${range(country.startYear, country.endInclusiveYear)}</td></tr>"
                }}
            </table>
            <table>
                <tr><th>설명</th><th>경로</th></tr>
                <tr><td>연도별</td><td><code>holiday/{country}/{year}.json</code></td></tr>
                <tr><td>월별</td><td><code>holiday/{country}/{year}-{month}.json</code></td></tr>
            </table>

            <h2>음력</h2>
            <p>원본 제공 범위 : <code>${meta.lunar.startYear} ~ ${meta.lunar.endInclusiveYear}</code>, 생성 완료 : <code>$lunarRange</code></p>
            <table>
                <tr><th>설명</th><th>경로</th></tr>
                <tr><td>연도별</td><td><code>lunar/{year}.json</code></td></tr>
                <tr><td>월별</td><td><code>lunar/{year}-{month}.json</code></td></tr>
            </table>

            <h2>통합</h2>
            <p>공휴일과 음력을 한 번에 받습니다.</p>
            <table>
                <tr><th>설명</th><th>경로</th></tr>
                <tr><td>연도별</td><td><code>calendar/{country}/{year}.json</code></td></tr>
                <tr><td>월별</td><td><code>calendar/{country}/{year}-{month}.json</code></td></tr>
            </table>

            <h2>메타</h2>
            <p><a href="meta.json">meta.json</a> 에서 갱신 시각과 실제 생성된 범위를 확인할 수 있습니다.</p>

            <p><a href="https://github.com/taetae98coding/CalendarApi">GitHub 저장소</a></p>
            </body>
            </html>
        """.trimIndent()
    }

    private fun range(start: Int?, endInclusive: Int?): String {
        if (start == null || endInclusive == null) return "없음"

        return "$start ~ $endInclusive"
    }

    private fun YearRange.text(): String {
        return if (start == endInclusive) {
            "$start"
        } else {
            "$start ~ $endInclusive"
        }
    }
}
