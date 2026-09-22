package io.github.taetae98coding.calendar.datasource.openapi.kasi

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** 한국천문연구원 음양력 API 의 윤달 표기("평" / "윤")를 Boolean 으로 변환한다. */
data object KasiLeapMonthSerializer : KSerializer<Boolean> {
    override val descriptor = PrimitiveSerialDescriptor("KasiLeapMonth", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Boolean) {
        val stringValue = if (value) {
            "윤"
        } else {
            "평"
        }

        encoder.encodeString(stringValue)
    }

    override fun deserialize(decoder: Decoder): Boolean {
        return when (val value = decoder.decodeString()) {
            "윤" -> true
            "평" -> false
            else -> error("Unexpected value : $value")
        }
    }
}
