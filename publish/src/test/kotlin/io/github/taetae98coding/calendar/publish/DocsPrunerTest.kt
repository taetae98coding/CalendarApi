package io.github.taetae98coding.calendar.publish

import io.github.taetae98coding.calendar.core.RecordingLogger
import io.github.taetae98coding.calendar.core.file.relativeFilePaths
import io.github.taetae98coding.calendar.core.file.tempDirectory
import io.github.taetae98coding.calendar.core.file.touch
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class DocsPrunerTest {
    private val root = tempDirectory()
    private val logger = RecordingLogger()

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun `루트 파일과 범위 안의 문서만 남긴다`() = runTest {
        root.touch("meta.json")
        root.touch("index.html")
        root.touch(".nojekyll")
        root.touch("README.md")
        root.touch("holiday/kr/2026.json")
        root.touch("holiday/kr/2026/01.json")
        root.touch("holiday/kr/1997.json")
        root.touch("holiday/kr/1997/01.json")
        root.touch("holiday/jp/2026.json")
        root.touch("legacy/kr/2026.json")

        DocsPruner(DocPaths(root), logger, years = 2025..2026).prune()

        assertEquals(setOf("meta.json", "index.html", ".nojekyll", "holiday/kr/2026.json", "holiday/kr/2026/01.json"), root.relativeFilePaths())
        assertEquals(5, logger.messages.size)
    }
}
