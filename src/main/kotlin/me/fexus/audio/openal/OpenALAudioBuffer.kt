package me.fexus.audio.openal

import org.lwjgl.openal.AL10.*
import java.nio.ByteBuffer


class OpenALAudioBuffer {
    val bufferID = alGenBuffers().catchALError()

    fun putData(data: ByteBuffer, format: Int, sampleRate: Int) {
        alBufferData(bufferID, format, data, sampleRate).catchALError()
    }

    fun destroy() {
        alDeleteBuffers(bufferID).catchALError()
    }
}