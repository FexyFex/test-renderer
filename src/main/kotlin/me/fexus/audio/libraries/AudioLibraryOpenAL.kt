package me.fexus.audio.libraries

import me.fexus.audio.*
import me.fexus.math.vec.DVec3
import me.fexus.math.vec.Vec3
import org.lwjgl.openal.AL
import org.lwjgl.openal.ALC.createCapabilities
import org.lwjgl.openal.ALC10.*
import java.nio.ByteBuffer
import java.nio.IntBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.Mixer
import javax.sound.sampled.spi.MixerProvider


class AudioLibraryOpenAL: AudioLibrary {
    override val libraryName: String = "OpenAL"
    override val libraryDescription: String = "A free audio library by Loki Software."

    private val openALDevice = alcOpenDevice(null as ByteBuffer?)
    private val openALContext = alcCreateContext(openALDevice, null as IntBuffer?)
    private val capabilities = createCapabilities(openALDevice)

    override var isInitialized: Boolean = false; private set

    override lateinit var listenerData: ListenerData


    override fun init() {
        if (openALDevice == 0L) throw IllegalStateException("Failed to open the default OpenAL device")
        if (openALContext == 0L) throw IllegalStateException("Failed to create OpenAL context")
        alcMakeContextCurrent(openALContext)
        AL.createCapabilities(capabilities)

        isInitialized = true
    }

    override fun createChannel(channelType: AudioChannel.Type, decoder: AudioDataDecoder): Channel {
        return Channel(channelType, decoder)
    }

    override fun createEmitter(): Emitter {
        return Emitter()
    }

    override fun shutdown() {
        isInitialized = false

        alcDestroyContext(openALContext)
        alcCloseDevice(openALDevice)
    }


    class Channel(override val type: AudioChannel.Type, override val decoder: AudioDataDecoder): AudioChannel {
        override lateinit var audioFormat: AudioFormat; private set
        override val timestamp: Float; get() = 0f

        override fun prepareBuffers(buffers: List<AudioBuffer>) {

        }

        override fun queueBuffer(buffer: AudioBuffer) {

        }

        override fun clear() {

        }
    }

    class Emitter(): SoundEmitter {
        override val position: DVec3 = DVec3(0.0)
        override val velocity: Vec3 = Vec3(0f)

        override var doLooping: Boolean = false
        override var gain: Float = 0f
        override val isPlaying: Boolean = false
        override var pitch: Float = 1f
        override var volume: Float = 1f

        override val queuedBuffers: List<AudioBuffer> = mutableListOf()

        override fun play(channel: AudioChannel) {

        }
    }
}