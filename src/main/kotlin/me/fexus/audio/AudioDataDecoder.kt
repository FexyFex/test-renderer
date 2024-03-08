package me.fexus.audio

import java.io.InputStream
import javax.sound.sampled.AudioFormat


interface AudioDataDecoder {
    val audioStream: InputStream
    val audioFormat: AudioFormat
    val isEndOfStream: Boolean
    val isInitialized: Boolean

    fun init(): AudioDataDecoder

    fun getFullAudioData(): AudioBuffer
    fun getAudioData(size: Int): AudioBuffer
}