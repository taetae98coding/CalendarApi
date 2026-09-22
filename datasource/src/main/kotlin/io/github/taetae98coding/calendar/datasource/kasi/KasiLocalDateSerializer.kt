package io.github.taetae98coding.calendar.datasource.kasi

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** 특일 정보의 `locdate` 표기(20250101). */
object KasiLocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("KasiLocalDate", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeInt(value.year * 10000 + value.month.number * 100 + value.day)
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        val value = decoder.decodeInt()

        return LocalDate(year = value / 10000, month = (value % 10000) / 100, day = value % 100)
    }
}
