package me.fexus.audio

import javax.sound.sampled.AudioFormat


class AudioBuffer(val data: ByteArray, val format: AudioFormat) {
    val size = data.size
}
