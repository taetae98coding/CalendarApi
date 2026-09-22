package io.github.taetae98coding.calendar.core

import io.github.taetae98coding.calendar.core.file.DirectoryPruner
import io.github.taetae98coding.calendar.core.file.DirectoryPruner.Decision
import io.github.taetae98coding.calendar.core.file.YearFileName
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class DirectoryPrunerTest {
    private val root = createTempDirectory("calendar-api-core").toFile()
    private val logs = mutableListOf<String>()

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun touch(path: String) {
        File(root, path).apply { parentFile.mkdirs() }.writeText("")
    }

    @Test
    fun `규칙이 정한 대로 남기고 내려가고 지운다`() = runTest {
        touch("keep/anything.txt")
        touch("descend/known.json")
        touch("descend/unknown.json")
        touch("delete/child.json")

        DirectoryPruner(Logger(logs::add)).prune(root) { path ->
            when {
                path.size == 1 && path[0] == "keep" -> Decision.KEEP
                path.size == 1 && path[0] == "descend" -> Decision.DESCEND
                path.size == 2 && path[1] == "known.json" -> Decision.KEEP
                else -> Decision.DELETE
            }
        }

        val remaining = root.walk().filter(File::isFile).map { file -> file.relativeTo(root).path.replace(File.separatorChar, '/') }.toSet()

        assertEquals(setOf("keep/anything.txt", "descend/known.json"), remaining)
        assertEquals(2, logs.size)
    }

    @Test
    fun `연도 항목 이름은 파일과 폴더 두 가지다`() {
        assertEquals(2026, YearFileName.yearOf("2026"))
        assertEquals(2026, YearFileName.yearOf("2026.json"))
        assertNull(YearFileName.yearOf("meta.json"))
        assertNull(YearFileName.yearOf("20260.json"))
        assertEquals("2026.json", YearFileName.file(2026))
        assertEquals("2026", YearFileName.directory(2026))
    }
}
