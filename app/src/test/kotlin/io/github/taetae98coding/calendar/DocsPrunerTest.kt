package io.github.taetae98coding.calendar

import io.github.taetae98coding.calendar.data.Logger
import io.github.taetae98coding.calendar.publish.DocPaths
import io.github.taetae98coding.calendar.publish.DocsPruner
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class DocsPrunerTest {
    private val root = createTempDirectory("calendar-api-docs").toFile()
    private val logs = mutableListOf<String>()

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun touch(path: String) {
        File(root, path).apply { parentFile.mkdirs() }.writeText("")
    }

    @Test
    fun `루트 파일과 범위 안의 문서만 남긴다`() = runTest {
        touch("meta.json")
        touch("index.html")
        touch(".nojekyll")
        touch("README.md")
        touch("holiday/kr/2026.json")
        touch("holiday/kr/2026/01.json")
        touch("holiday/kr/1997.json")
        touch("holiday/kr/1997/01.json")
        touch("holiday/jp/2026.json")
        touch("legacy/kr/2026.json")

        DocsPruner(DocPaths(root), Logger(logs::add), years = 2025..2026).prune()

        val remaining = root.walk().filter(File::isFile).map { file -> file.relativeTo(root).path.replace(File.separatorChar, '/') }.toSet()

        assertEquals(setOf("meta.json", "index.html", ".nojekyll", "holiday/kr/2026.json", "holiday/kr/2026/01.json"), remaining)
        assertEquals(5, logs.size)
    }
}
