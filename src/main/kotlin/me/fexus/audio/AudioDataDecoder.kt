package me.fexus.audio

import java.io.InputStream
import javax.sound.sampled.AudioFormat


interface AudioDataDecoder {
    val audioStream: InputStream
    val audioFormat: AudioFormat
    val isInitialized: Boolean
    val audioDataSize: Int

    fun init(): AudioDataDecoder

    fun getFullAudioData(): AudioBuffer
    fun getAudioData(offset: Int, size: Int): AudioBuffer
}