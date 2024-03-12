package me.fexus.audio.openal

import org.lwjgl.openal.AL10
import java.nio.ByteBuffer


fun findOpenALFormat(channelCount: Int, bitsPerSample: Int): Int {
    // IMPORTANT:
    //  For OpenAL to support spatial dislocation of sound, the audio must be single channel (MONO) only!!!
    return when {
        channelCount == 1 && bitsPerSample == 8 -> AL10.AL_FORMAT_MONO8
        channelCount == 1 && bitsPerSample == 16 -> AL10.AL_FORMAT_MONO16
        channelCount > 1 && bitsPerSample == 8 -> AL10.AL_FORMAT_STEREO8
        channelCount > 1 && bitsPerSample == 16 -> AL10.AL_FORMAT_STEREO16
        else -> throw Exception("No matching format found for channel count = $channelCount and $bitsPerSample bit per sample")
    }
}


fun ByteArray.toByteBuffer(): ByteBuffer {
    val buf = ByteBuffer.allocateDirect(this.size)
    this.forEachIndexed { index, byte ->
        buf.put(index, byte)
    }
    return buf
}