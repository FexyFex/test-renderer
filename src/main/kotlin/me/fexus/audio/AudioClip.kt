package me.fexus.audio

import javax.sound.sampled.AudioFormat


class AudioClip(val type: Type, val decoder: AudioDataDecoder) {
    val audioFormat: AudioFormat = decoder.audioFormat
    val timestamp: Float = 0f
    var offset: Int = 0; private set
    var isEndOfStream: Boolean = false; private set


    fun getAudioData(size: Int): AudioBuffer {
        val buf = decoder.getAudioData(this.offset, size)
        this.offset += size
        if (this.offset >= decoder.audioDataSize) isEndOfStream = true
        return buf
    }

    fun getFullAudioData(): AudioBuffer {
        val buf = decoder.getFullAudioData()
        this.offset += buf.size
        this.isEndOfStream = true
        return buf
    }

    fun rewindTo(offset: Int) {
        this.offset = offset
        this.isEndOfStream = false
    }

    fun rewind() = rewindTo(0)

    enum class Type(val id: Int) {
        ALL_AT_ONCE(0),
        STREAMING(1)
    }
}