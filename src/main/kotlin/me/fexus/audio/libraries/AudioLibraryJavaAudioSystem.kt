package me.fexus.audio.libraries

import me.fexus.audio.*
import me.fexus.audio.AudioLibrary.Companion.assertType
import me.fexus.math.vec.DVec3
import me.fexus.math.vec.Vec3
import javax.sound.sampled.AudioFormat


class AudioLibraryJavaAudioSystem: AudioLibrary {
    override val libraryName: String = "Java AudioSystem"
    override val libraryDescription: String = "The default Java audio library."

    override var isInitialized: Boolean = false; private set

    override lateinit var listenerData: ListenerData


    override fun init() {
        isInitialized = true
    }

    override fun createChannel(channelType: AudioChannel.Type): Channel {
        assertInitialized()
        return Channel(channelType)
    }

    override fun createEmitter(): Emitter {
        assertInitialized()
        return Emitter()
    }

    override fun shutdown() {
        isInitialized = false
    }

    class Channel(override val type: AudioChannel.Type): AudioChannel {
        override lateinit var audioFormat: AudioFormat; private set
        override val timestamp: Float; get() = 0f

        init {

        }

        override fun prepareBuffers(buffers: List<AudioBuffer>) {

        }

        override fun queueBuffer(buffer: AudioBuffer) {

        }

        override fun clear() {

        }
    }

    class Emitter: SoundEmitter {
        override val position: DVec3 = DVec3(0.0)
        override val velocity: Vec3 = Vec3(0f)

        override var doLooping: Boolean = false
        override var volume: Float = 1f
        override var gain: Float = 1f
        override var isPlaying: Boolean = false
        override var pitch: Float = 1f

        override lateinit var queuedBuffers: List<AudioBuffer>


        override fun play(channel: AudioChannel) {
            val validatedChannel = channel.assertType(::Channel)


        }
    }
}