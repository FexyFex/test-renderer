package me.fexus.audio.decoder

import me.fexus.audio.AudioBuffer
import me.fexus.audio.AudioDataDecoder
import me.fexus.math.clamp
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem


class AudioDecoderWAV(override val audioStream: AudioInputStream): AudioDataDecoder {
    override lateinit var audioFormat: AudioFormat; private set
    override val isInitialized: Boolean; get() = this::audioFormat.isInitialized
    private val unpackedAudioData = audioStream.readAllBytes()
    override val audioDataSize: Int = unpackedAudioData.size


    override fun init(): AudioDecoderWAV {
        this.audioFormat = this.audioStream.format
        return this
    }

    override fun getFullAudioData(): AudioBuffer {
        return AudioBuffer(unpackedAudioData, this.audioFormat)
    }

    override fun getAudioData(offset: Int, size: Int): AudioBuffer {
        val start = offset.clamp(0, unpackedAudioData.size)
        val end = (offset + size).clamp(offset, unpackedAudioData.size)
        val slice = unpackedAudioData.slice(start until end).toByteArray()

        return AudioBuffer(slice, this.audioFormat)
    }

}