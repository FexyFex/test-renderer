package me.fexus.audio

import javax.sound.sampled.AudioFormat


interface AudioChannel {
    val type: Type
    val audioFormat: AudioFormat
    val timestamp: Float
    val decoder: AudioDataDecoder


    fun prepareBuffers(buffers: List<AudioBuffer>)
    fun queueBuffer(buffer: AudioBuffer)

    fun clear()

    enum class Type(val id: Int) {
        ALL_AT_ONCE(0),
        STREAMING(1)
    }
}