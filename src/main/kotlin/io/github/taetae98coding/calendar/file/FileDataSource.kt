package io.github.taetae98coding.calendar.file

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

@OptIn(ExperimentalSerializationApi::class)
data object FileDataSource {
    val printJson by lazy {
        Json {
            prettyPrint = true
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
