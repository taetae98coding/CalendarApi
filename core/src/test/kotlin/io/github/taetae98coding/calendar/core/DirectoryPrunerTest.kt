package io.github.taetae98coding.calendar.core

import io.github.taetae98coding.calendar.core.file.DirectoryPruner
import io.github.taetae98coding.calendar.core.file.DirectoryPruner.Decision
import io.github.taetae98coding.calendar.core.file.relativeFilePaths
import io.github.taetae98coding.calendar.core.file.tempDirectory
import io.github.taetae98coding.calendar.core.file.touch
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class DirectoryPrunerTest {
    private val root = tempDirectory()
    private val logger = RecordingLogger()

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun `규칙이 정한 대로 남기고 내려가고 지운다`() = runTest {
        root.touch("keep/anything.txt")
        root.touch("descend/known.json")
        root.touch("descend/unknown.json")
        root.touch("delete/child.json")

        DirectoryPruner(logger).prune(root) { path ->
            when {
                path.size == 1 && path[0] == "keep" -> Decision.KEEP
                path.size == 1 && path[0] == "descend" -> Decision.DESCEND
                path.size == 2 && path[1] == "known.json" -> Decision.KEEP
                else -> Decision.DELETE
            }
        }

        assertEquals(setOf("keep/anything.txt", "descend/known.json"), root.relativeFilePaths())
        assertEquals(2, logger.messages.size)
    }
}
