package io.github.taetae98coding.calendar.datasource.file

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

@OptIn(ExperimentalSerializationApi::class)
data object FileDataSource {
    /** docs/ 로 배포되는 API 산출물. 사람이 읽고 diff 로 확인하므로 들여쓴다. */
    val printJson by lazy {
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
    }

    /**
     * cache/ 에 남기는 응답 원본. 내용은 그대로 두되 공백 없이 저장한다.
     * 기계만 읽고 한 번 쓰면 바뀌지 않는데, 음력 전체 범위 기준으로 들여쓰기가 158MB 대 62MB 차이를 만든다.
     */
    val cacheJson by lazy {
        Json {
            ignoreUnknownKeys = true
        }
    }

    suspend inline fun <reified T> write(value: T, file: File) {
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            file.outputStream()
                .buffered()
                .use { stream -> printJson.encodeToStream(value, stream) }
        }
    }

    suspend inline fun <reified T> writeCache(value: T, file: File) {
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            file.outputStream()
                .buffered()
                .use { stream -> cacheJson.encodeToStream(value, stream) }
        }
    }

    suspend inline fun <reified T> read(file: File): T {
        return withContext(Dispatchers.IO) {
            file.inputStream()
                .buffered()
                .use { stream -> printJson.decodeFromStream(stream) }
        }
    }

    suspend inline fun <reified T> readOrNull(file: File): T? {
        if (!file.exists()) return null

        return runCatching { read<T>(file) }.getOrNull()
    }

    suspend fun writeText(text: String, file: File) {
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            file.writeText(text)
        }
    }
}
