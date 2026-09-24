package io.github.taetae98coding.calendar.publish

import io.github.taetae98coding.calendar.core.FixedClock
import io.github.taetae98coding.calendar.core.file.tempDirectory
import io.github.taetae98coding.calendar.domain.Country
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

/** `meta.json` 은 소비처가 읽는 공개 규격이다. 모양이 바뀌면 여기서 먼저 걸린다. */
class MetaWriterTest {
    private val root = tempDirectory()
    private val paths = DocPaths(root)
    private val clock = FixedClock(Instant.parse("2026-09-24T00:00:00Z"))
    private val catalog = SourceCatalog { api, country -> listOf("${api.id}:${country.code}") }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun `갱신 시각과 고정 범위, 생성 현황, 출처를 적는다`() = runTest {
        val coverage = Coverage.Builder().apply {
            add(DocApi.HOLIDAY, Country.KOREA, 2025)
            add(DocApi.HOLIDAY, Country.KOREA, 2026)
        }.build()

        val written = MetaWriter(paths, catalog, clock, years = 2025..2027).write(coverage)
        val read = Json.decodeFromString<ApiMeta>(paths.meta.readText())

        assertEquals(written, read)
        assertEquals(clock.now, read.updatedAt)
        assertEquals(2025, read.startYear)
        assertEquals(2027, read.endInclusiveYear)
        assertEquals(listOf("holiday", "lunar", "calendar"), read.apis.map(ApiMeta.Api::id))

        val korea = read.apis.first().countries.first { country -> country.code == "kr" }
        assertEquals("대한민국", korea.name)
        assertEquals(listOf(ApiMeta.YearRange(2025, 2026)), korea.generated)
        assertEquals(listOf(ApiMeta.YearRange(2027, 2027)), korea.missing)
        assertEquals(listOf("holiday:kr"), korea.sources)
    }
}
