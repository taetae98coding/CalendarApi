package io.github.taetae98coding.calendar.core.file

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

/**
 * JSON 파일을 읽고 쓴다. 어떤 [json] 으로 쓰는지가 인스턴스의 정체다.
 *
 * - [pretty] : docs/ 로 배포되는 API 산출물. 사람이 읽고 diff 로 확인하므로 들여쓴다.
 * - [compact] : cache/ 에 남기는 응답 원본. 기계만 읽고 한 번 쓰면 바뀌지 않는데,
 *   음력 전체 범위 기준으로 들여쓰기가 158MB 대 62MB 차이를 만든다.
 */
@OptIn(ExperimentalSerializationApi::class)
class JsonFiles(
    val json: Json,
) {
    suspend inline fun <reified T> write(value: T, file: File) {
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            file.outputStream()
                .buffered()
                .use { stream -> json.encodeToStream(value, stream) }
        }
    }

    suspend inline fun <reified T> read(file: File): T {
        return withContext(Dispatchers.IO) {
            file.inputStream()
                .buffered()
                .use { stream -> json.decodeFromStream(stream) }
        }
    }

    /** 파일이 없으면 null. 있는데 깨졌으면 예외를 그대로 낸다. 손상은 삼키지 않고 호출자가 알아야 한다. */
    suspend inline fun <reified T> readOrNull(file: File): T? {
        val exists = withContext(Dispatchers.IO) { file.exists() }
        if (!exists) return null

        return read<T>(file)
    }

    suspend fun writeText(text: String, file: File) {
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            file.writeText(text)
        }
    }

    companion object {
        val pretty: JsonFiles = JsonFiles(
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            },
        )

        val compact: JsonFiles = JsonFiles(
            Json {
                ignoreUnknownKeys = true
            },
        )
    }
}
