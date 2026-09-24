package io.github.taetae98coding.calendar.data

/** 원천 응답 원본 모양의 문자열. 캐시에 그대로 넣어 저장소가 읽는 것을 확인한다. */
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
