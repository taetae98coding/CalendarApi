package io.github.taetae98coding.calendar.datasource.kasi

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** KASI 는 참·거짓을 두 글자 토큰으로 표기한다. 어떤 토큰 쌍인지만 다르고 해석 방식은 같다. */
abstract class KasiTokenSerializer(
    name: String,
    private val trueToken: String,
    private val falseToken: String,
) : KSerializer<Boolean> {
    override val descriptor = PrimitiveSerialDescriptor(name, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeString(if (value) trueToken else falseToken)
    }

    override fun deserialize(decoder: Decoder): Boolean {
        return when (val value = decoder.decodeString().trim()) {
            trueToken -> true
            falseToken -> false
            else -> error("Unexpected value : $value")
        }
    }
}

/** 특일 정보의 `isHoliday` 표기("Y" / "N"). */
object KasiBooleanSerializer : KasiTokenSerializer("KasiBoolean", trueToken = "Y", falseToken = "N")

/** 음양력 정보의 윤달 표기("윤" / "평"). */
object KasiLeapMonthSerializer : KasiTokenSerializer("KasiLeapMonth", trueToken = "윤", falseToken = "평")
