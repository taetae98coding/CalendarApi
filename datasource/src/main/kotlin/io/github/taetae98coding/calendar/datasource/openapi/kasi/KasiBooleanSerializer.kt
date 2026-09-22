package io.github.taetae98coding.calendar.datasource.openapi.kasi

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

data object KasiBooleanSerializer : KSerializer<Boolean> {
    override val descriptor = PrimitiveSerialDescriptor("KasiBoolean", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Boolean) {
        val stringValue = if (value) {
            "Y"
        } else {
            "N"
        }

        encoder.encodeString(stringValue)
    }

    override fun deserialize(decoder: Decoder): Boolean {
        return when (val value = decoder.decodeString().uppercase()) {
            "Y" -> true
            "N" -> false
            else -> error("Unexpected value : $value")
        }
    }
}
