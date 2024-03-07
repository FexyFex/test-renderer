package me.fexus.audio.decoder

import me.fexus.audio.AudioBuffer
import me.fexus.audio.IAudioDataDecoder
import me.fexus.audio.decoder.wav.HeaderWAVExtension
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem


class AudioDecoderWAV(override val fileData: ByteArray): IAudioDataDecoder {
    private lateinit var audioStream: AudioInputStream
    override lateinit var audioFormat: AudioFormat; private set
    override var isEndOfStream: Boolean = false; private set


    override fun init(): AudioDecoderWAV {
        this.audioStream = AudioSystem.getAudioInputStream(this.fileData.inputStream())
        this.audioFormat = this.audioStream.format

        return this
    }

    override fun isInitialized(): Boolean {
        return this::audioStream.isInitialized && this::audioFormat.isInitialized
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