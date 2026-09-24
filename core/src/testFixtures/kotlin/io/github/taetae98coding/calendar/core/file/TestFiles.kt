package io.github.taetae98coding.calendar.core.file

import java.io.File
import kotlin.io.path.createTempDirectory

/** 테스트마다 비어 있는 임시 폴더를 쓰고, 끝나면 [File.deleteRecursively] 로 지운다. */
fun tempDirectory(prefix: String = "calendar-api"): File = createTempDirectory(prefix).toFile()

/** 상대 경로에 파일을 만든다. 중간 폴더도 함께 만든다. */
fun File.touch(relativePath: String, text: String = ""): File {
    return File(this, relativePath).apply {
        parentFile.mkdirs()
        writeText(text)
    }
}

/** 이 폴더 아래 모든 파일의 상대 경로. OS 와 무관하게 `/` 로 잇는다. */
fun File.relativeFilePaths(): Set<String> {
    return walk()
        .filter(File::isFile)
        .map { file -> file.relativeTo(this).invariantPath() }
        .toSet()
}

/** OS 와 무관하게 `/` 로 이은 경로 문자열. */
fun File.invariantPath(): String = path.replace(File.separatorChar, '/')
