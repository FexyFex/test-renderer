package me.fexus.audio.libraries

import me.fexus.audio.*
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

    override fun createChannel(channelType: AudioChannel.Type): Channel {
        return Channel(channelType, )
    }

    override fun createEmitter(): Emitter {
        return Emitter()
    }

    override fun shutdown() {
        isInitialized = false
    }


    class Channel(override val type: AudioChannel.Type): AudioChannel {
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

    }
}