package me.fexus.audio

import me.fexus.audio.decoder.AudioDecoderOGG
import me.fexus.audio.decoder.AudioDecoderWAV
import kotlin.reflect.KClass


enum class AudioFileFormat(val decoder: KClass<*>) {
    OGG(AudioDecoderOGG::class),
    WAV(AudioDecoderWAV::class)
}