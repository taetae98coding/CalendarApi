package io.github.taetae98coding.calendar.data

import io.github.taetae98coding.calendar.data.cache.CachePaths
import io.github.taetae98coding.calendar.data.cache.CachePruner
import java.io.File
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

    private fun touch(path: String) {
        File(root, path).apply { parentFile.mkdirs() }.writeText("{}")
    }

    @Test
    fun `아는 이름만 남기고 나머지는 지운다`() = runTest {
        touch("kasi/getLunCalInfo/2026/01.json")
        touch("kasi/getLunCalInfo/meta.json")
        touch("kasi/getLunCalInfo/1997/01.json") // 범위 밖
        touch("kasi/getLunCalInfo/2026.json") // 월 단위 API 에 연 파일
        touch("kasi/unknownApi/2026/01.json")
        touch("nager/publicHolidays-kr/2026.json")
        touch("nager/publicHolidays-kr/2026/01.json") // 연 단위 API 에 월 폴더
        touch("unknownProvider/x.json")

        CachePruner(CachePaths(root), logger, years = 2025..2026).prune()

        val remaining = root.walk().filter(File::isFile).map { file -> file.relativeTo(root).path.replace(File.separatorChar, '/') }.toSet()

        assertEquals(setOf("kasi/getLunCalInfo/2026/01.json", "kasi/getLunCalInfo/meta.json", "nager/publicHolidays-kr/2026.json"), remaining)
        assertEquals(5, logger.messages.size)
    }
}
