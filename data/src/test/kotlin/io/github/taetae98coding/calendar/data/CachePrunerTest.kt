package io.github.taetae98coding.calendar.data

import io.github.taetae98coding.calendar.core.RecordingLogger
import io.github.taetae98coding.calendar.core.file.relativeFilePaths
import io.github.taetae98coding.calendar.core.file.tempDirectory
import io.github.taetae98coding.calendar.core.file.touch
import io.github.taetae98coding.calendar.data.cache.CachePaths
import io.github.taetae98coding.calendar.data.cache.CachePruner
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class CachePrunerTest {
    private val root = tempDirectory()
    private val logger = RecordingLogger()

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun `아는 이름만 남기고 나머지는 지운다`() = runTest {
        root.touch("kasi/getLunCalInfo/2026/01.json")
        root.touch("kasi/getLunCalInfo/meta.json")
        root.touch("kasi/getLunCalInfo/1997/01.json") // 범위 밖
        root.touch("kasi/getLunCalInfo/2026.json") // 월 단위 API 에 연 파일
        root.touch("kasi/unknownApi/2026/01.json")
        root.touch("nager/publicHolidays-kr/2026.json")
        root.touch("nager/publicHolidays-kr/2026/01.json") // 연 단위 API 에 월 폴더
        root.touch("unknownProvider/x.json")

        CachePruner(CachePaths(root), logger, years = 2025..2026).prune()

        assertEquals(setOf("kasi/getLunCalInfo/2026/01.json", "kasi/getLunCalInfo/meta.json", "nager/publicHolidays-kr/2026.json"), root.relativeFilePaths())
        assertEquals(5, logger.messages.size)
    }
}
