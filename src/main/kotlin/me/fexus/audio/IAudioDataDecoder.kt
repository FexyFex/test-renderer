package me.fexus.audio

import javax.sound.sampled.AudioFormat


interface IAudioDataDecoder {
    val fileData: ByteArray
    val audioFormat: AudioFormat
    val isEndOfStream: Boolean

    fun init(): IAudioDataDecoder
    fun isInitialized(): Boolean

    fun getFullAudioData(): AudioBuffer
    fun getAudioData(size: Int): AudioBuffer
}