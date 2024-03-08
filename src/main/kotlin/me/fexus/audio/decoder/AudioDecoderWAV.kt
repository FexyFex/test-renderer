package me.fexus.audio.decoder

import me.fexus.audio.AudioBuffer
import me.fexus.audio.AudioDataDecoder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem


class AudioDecoderWAV(override val audioStream: AudioInputStream): AudioDataDecoder {
    constructor(audioData: ByteArray): this(AudioSystem.getAudioInputStream(audioData.inputStream()))

    override lateinit var audioFormat: AudioFormat; private set
    override var isEndOfStream: Boolean = false; private set
    override val isInitialized: Boolean; get() = this::audioFormat.isInitialized


    override fun init(): AudioDecoderWAV {
        this.audioFormat = this.audioStream.format

        return this
    }

    override fun getFullAudioData(): AudioBuffer {
        this.isEndOfStream = true
        return AudioBuffer(this.audioStream.readAllBytes(), this.audioFormat)
    }

    override fun getAudioData(size: Int): AudioBuffer {
        val arr = ByteArray(size)
        val bytesRead = this.audioStream.read(arr, 0, size)

        if (bytesRead == -1) this.isEndOfStream = true

        return AudioBuffer(arr, this.audioFormat)
    }

}