package io.github.taetae98coding.calendar.core.file

import io.github.taetae98coding.calendar.core.Logger
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 폴더 트리에서 아는 이름만 남기고 나머지는 지운다.
 *
 * 캐시와 배포 문서는 실행이 만드는 산출물이라, 범위나 폴더 구조가 바뀌어도 이전 실행이 남긴 파일이 그대로 남아서는 안 된다.
 * 트리의 모양은 [Rule] 이 정하고 여기는 내려가며 지우는 일만 한다. 루트 자체는 지우지 않는다.
 */
class DirectoryPruner(
    private val logger: Logger,
) {
    suspend fun prune(root: File, rule: Rule) {
        withContext(Dispatchers.IO) {
            root.children().forEach { child -> visit(child, listOf(child.name), rule) }
        }
    }

    private fun visit(file: File, path: List<String>, rule: Rule) {
        when (rule.decide(path)) {
            Decision.KEEP -> Unit
            Decision.DELETE -> delete(file)
            Decision.DESCEND -> file.children().forEach { child -> visit(child, path + child.name, rule) }
        }
    }

    private fun File.children(): List<File> = listFiles().orEmpty().toList()

    private fun delete(file: File) {
        logger.log("[Prune] ${file.path} 삭제")
        file.deleteRecursively()
    }

    /** 루트 기준 상대 경로 조각([path])을 보고 항목 하나의 처분을 정한다. 마지막 조각이 지금 보는 항목이다. */
    fun interface Rule {
        fun decide(path: List<String>): Decision
    }

    enum class Decision {
        /** 이 항목과 그 아래를 그대로 둔다. */
        KEEP,

        /** 이 항목을 통째로 지운다. */
        DELETE,

        /** 이 항목은 두고 아래 항목을 하나씩 다시 묻는다. */
        DESCEND,
    }
}
