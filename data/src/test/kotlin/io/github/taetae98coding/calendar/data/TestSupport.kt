package io.github.taetae98coding.calendar.data

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.time.Clock
import kotlin.time.Instant

/** 테스트마다 비어 있는 임시 폴더를 쓰고, 끝나면 지운다. */
fun tempDirectory(): File = createTempDirectory("calendar-api").toFile()

/** 시각이 고정된 시계. 갱신 기록에 찍히는 값을 예측할 수 있다. */
class FixedClock(
    var now: Instant,
) : Clock {
    override fun now(): Instant = now
}

/** 로그를 모아 두고 확인한다. */
class RecordingLogger : Logger {
    val messages = mutableListOf<String>()

    override fun log(message: String) {
        messages += message
    }
}

object Fixtures {
    fun kasiEnvelope(items: String, count: Int): String {
        return """{"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},"body":{"items":$items,"numOfRows":100,"pageNo":1,"totalCount":$count}}}"""
    }

    fun spcde(vararg items: String): String {
        return when (items.size) {
            1 -> kasiEnvelope("""{"item":${items.single()}}""", 1)
            else -> kasiEnvelope("""{"item":[${items.joinToString(",")}]}""", items.size)
        }
    }

    fun spcdeItem(name: String, date: Int, isHoliday: Boolean, kind: String = "01"): String {
        return """{"dateKind":"$kind","dateName":"$name","isHoliday":"${if (isHoliday) "Y" else "N"}","locdate":$date,"seq":1}"""
    }

    fun lunar(vararg items: String): String {
        return when (items.size) {
            1 -> kasiEnvelope("""{"item":${items.single()}}""", 1)
            else -> kasiEnvelope("""{"item":[${items.joinToString(",")}]}""", items.size)
        }
    }

    fun lunarItem(solar: String, lunarYear: Int, lunarMonth: Int, lunarDay: Int, leap: Boolean = false): String {
        val (year, month, day) = solar.split("-")

        return """{"solYear":"$year","solMonth":"$month","solDay":"$day","lunYear":"$lunarYear","lunMonth":"${lunarMonth.toString().padStart(2, '0')}","lunDay":"${lunarDay.toString().padStart(2, '0')}","lunLeapmonth":"${if (leap) "윤" else "평"}"}"""
    }

    fun nager(vararg items: String): String = "[${items.joinToString(",")}]"

    fun nagerItem(date: String, localName: String, types: String = "\"Public\"", counties: String = "null", global: Boolean = true): String {
        return """{"date":"$date","localName":"$localName","name":"$localName","countryCode":"KR","global":$global,"counties":$counties,"types":[$types]}"""
    }
}
