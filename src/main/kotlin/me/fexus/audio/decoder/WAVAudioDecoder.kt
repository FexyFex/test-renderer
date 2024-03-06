package me.fexus.audio.decoder

import me.fexus.audio.AudioBuffer
import me.fexus.audio.IAudioDataDecoder
import me.fexus.audio.decoder.wav.HeaderWAVExtension
import me.fexus.audio.decoder.wav.HeaderWAVExtensionLIST
import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem


class WAVAudioDecoder(override val fileData: ByteArray): IAudioDataDecoder {
    private lateinit var audioStream: AudioInputStream
    override lateinit var audioFormat: AudioFormat; private set
    override var isEndOfStream: Boolean = false; private set

    private val extensions = mutableListOf<HeaderWAVExtension>()


    override fun init() {
        this.audioStream = AudioSystem.getAudioInputStream(fileData.inputStream())
        this.audioFormat = audioStream.format
    }

    override fun isInitialized(): Boolean {
        return this::audioStream.isInitialized && this::audioFormat.isInitialized
    }

    override fun getFullAudioData(): AudioBuffer {
        isEndOfStream = true
        return AudioBuffer(this.audioStream.readAllBytes(), this.audioFormat)
    }

    override fun getAudioData(size: Int): AudioBuffer {
        val arr = ByteArray(size)
        val bytesRead = audioStream.read(arr, 0, size)

        if (bytesRead == -1) isEndOfStream = true

        return AudioBuffer(arr, this.audioFormat)
    }

}