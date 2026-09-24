package io.github.taetae98coding.calendar.core.file

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** JSON 이 아닌 텍스트 파일을 쓴다. 배포 루트의 `index.html` 과 `.nojekyll` 이 여기를 지난다. */
object TextFiles {
    suspend fun write(text: String, file: File) {
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            file.writeText(text)
        }
    }
}
