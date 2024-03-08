package me.fexus.audio.openal

import me.fexus.math.vec.Vec3
import org.lwjgl.openal.AL10.*


class OpenALAudioSource {
    private val sourceID: Int
    val buffersProcessed: Int
        get() {
            val arr = IntArray(1) { -1 }
            alGetSourcei(sourceID, AL_BUFFERS_PROCESSED, arr).catchALError()
            return arr.first()
        }


    init {
        //OpenALAudioSystem
        sourceID = alGenSources().catchALError()
    }


    fun setBuffer(buffer: OpenALAudioBuffer): OpenALAudioSource {
        stop()
        alSourcei(sourceID, AL_BUFFER, buffer.bufferID).catchALError()
        return this
    }

    fun setPosition(pos: Vec3): OpenALAudioSource {
        alSource3f(sourceID, AL_POSITION, pos.x, pos.y, pos.z).catchALError()
        return this
    }

    fun setVelocity(velocity: Vec3): OpenALAudioSource {
        alSource3f(sourceID, AL_VELOCITY, velocity.x, velocity.y, velocity.z).catchALError()
        return this
    }

    fun setGain(gain: Float): OpenALAudioSource {
        alSourcef(sourceID, AL_GAIN, gain).catchALError()
        return this
    }

    fun setPitch(pitchValue: Float): OpenALAudioSource {
        alSourcef(sourceID, AL_PITCH, pitchValue).catchALError()
        return this
    }

    fun setLooping(state: Boolean): OpenALAudioSource {
        val alEnum = if (state) AL_TRUE else AL_FALSE
        alSourcei(sourceID, AL_LOOPING, alEnum).catchALError()
        return this
    }

    fun setProperty(property: Int, value: Float): OpenALAudioSource {
        alSourcef(sourceID, property, value).catchALError()
        return this
    }


    fun queueBuffers(buffers: Array<OpenALAudioBuffer>) =
        alSourceQueueBuffers(sourceID, buffers.map { it.bufferID }.toIntArray()).catchALError()

    fun unqueueBuffers(buffers: IntArray) = alSourceUnqueueBuffers(sourceID, buffers).catchALError()


    fun isPlaying(): Boolean = alGetSourcei(sourceID, AL_SOURCE_STATE).catchALError() == AL_PLAYING

    fun play() = alSourcePlay(sourceID).catchALError()
    fun pause() = alSourcePause(sourceID).catchALError()
    fun stop() = alSourceStop(sourceID).catchALError()

    fun destroy() {
        stop()
        alDeleteSources(sourceID).catchALError()
    }
}