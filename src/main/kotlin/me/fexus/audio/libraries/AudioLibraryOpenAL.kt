package me.fexus.audio.libraries

import me.fexus.audio.*
import me.fexus.audio.AudioLibrary.Companion.assertType
import me.fexus.audio.openal.catchALError
import me.fexus.audio.openal.findOpenALFormat
import me.fexus.audio.openal.toByteBuffer
import me.fexus.math.vec.Vec3
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.ALC.createCapabilities
import org.lwjgl.openal.ALC10.*
import org.lwjgl.openal.ALCCapabilities
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.concurrent.atomic.AtomicBoolean


@Suppress("unused")
class AudioLibraryOpenAL: AudioLibrary {
    override val libraryName: String = "OpenAL"
    override val libraryDescription: String = "A free audio library by Loki Software."

    private var openALDevice: Long = 0L
    private var openALContext: Long = 0L
    private lateinit var capabilities: ALCCapabilities

    override var isInitialized: Boolean = false; private set

    override var listenerData = ListenerData(Vec3(0f), Vec3(0f), Vec3(0f, 1f, 0f), Vec3(0f))
        set(value) {
            alListener3f(AL_POSITION, value.position.x, value.position.y, value.position.z).catchALError()
            alListener3f(AL_VELOCITY, value.velocity.x, value.velocity.y, value.velocity.z).catchALError()

            val buf = floatArrayOf(
                value.rotation.x, value.rotation.y, value.rotation.z,
                value.up.x, value.up.y, value.up.z
            )
            alListenerfv(AL_ORIENTATION, buf).catchALError()

            field = value
        }


    override fun init() {
        openALDevice = alcOpenDevice(null as ByteBuffer?)
        if (openALDevice == 0L) throw IllegalStateException("Failed to open the default OpenAL device")

        openALContext = alcCreateContext(openALDevice, null as IntBuffer?)
        if (openALContext == 0L) throw IllegalStateException("Failed to create OpenAL context")

        this.capabilities = createCapabilities(openALDevice)

        alcMakeContextCurrent(openALContext)
        AL.createCapabilities(capabilities)

        isInitialized = true
    }

    override fun createClip(channelType: AudioClip.Type, decoder: AudioDataDecoder): AudioClip {
        return AudioClip(channelType, decoder)
    }

    override fun createEmitter(): Emitter {
        return Emitter()
    }

    override fun setListenerPosition(position: Vec3) {
        alListener3f(AL_POSITION, position.x, position.y, position.z).catchALError()
        listenerData.position.x = position.x
        listenerData.position.y = position.y
        listenerData.position.z = position.z
    }

    override fun setListenerRotation(up: Vec3, rotation: Vec3) {
        val buf = floatArrayOf(rotation.x, rotation.y, rotation.z, up.x, up.y, up.z)
        alListenerfv(AL_ORIENTATION, buf).catchALError()
        listenerData.rotation.x = rotation.x
        listenerData.rotation.y = rotation.y
        listenerData.rotation.z = rotation.z
        listenerData.up.x = up.x
        listenerData.up.y = up.y
        listenerData.up.z = up.z
    }

    override fun setListenerVelocity(velocity: Vec3) {
        alListener3f(AL_VELOCITY, velocity.x, velocity.y, velocity.z).catchALError()
        listenerData.velocity.x = velocity.x
        listenerData.velocity.y = velocity.y
        listenerData.velocity.z = velocity.z
    }

    override fun shutdown() {
        alcDestroyContext(openALContext)
        alcCloseDevice(openALDevice)
        isInitialized = false
    }


    class Emitter: SoundEmitter {
        override val isPlaying: Boolean; get() = alGetSourcei(sourceID, AL_SOURCE_STATE).catchALError() == AL_PLAYING
        override var doLooping: Boolean = false
        override var gain: Float = 0f
        override var pitch: Float = 1f

        override lateinit var currentClip: AudioClip
        private val hasNextClip = AtomicBoolean(false)
        private var nextClip: AudioClip? = null
        private var openALFormat: Int = -1
        private var sampleRate: Int = -1

        override var bufferSize: Int = FexAudioSystem.DEFAULT_BUFFER_SIZE
        private var queuedBuffers = Array(3) { alGenBuffers().catchALError() }

        override val readyToPlay: AtomicBoolean = AtomicBoolean(false)

        private var sourceID: Int = -1

        private val playRequested = AtomicBoolean(false)
        private val pauseRequested = AtomicBoolean(false)
        private val stopRequested = AtomicBoolean(false)


        override fun setPosition(position: Vec3) {
            alSource3f(sourceID, AL_POSITION, position.x, position.y, position.z).catchALError()
        }

        override fun setVelocity(velocity: Vec3) {
            alSource3f(sourceID, AL_VELOCITY, velocity.x, velocity.y, velocity.z).catchALError()
        }

        override fun play(clip: AudioClip) {
            if (this.hasNextClip.get()) return
            this.nextClip = clip
            this.hasNextClip.set(true)
            play()
        }

        override fun play() {
            this.playRequested.set(true)
            this.pauseRequested.set(false)
            this.stopRequested.set(false)
        }

        override fun pause() {
            this.pauseRequested.set(true)
        }

        override fun stop() {
            this.stopRequested.set(true)
        }

        private fun prepareNewClip() {
            val clip = this.nextClip!!

            if (this::currentClip.isInitialized && clip == this.currentClip) return
            if (!this::currentClip.isInitialized || clip != this.currentClip)
                this.currentClip = clip

            if (this.sourceID <= -1)
                this.sourceID = alGenSources().catchALError()

            val channels = clip.audioFormat.channels
            val bitsPerSample = clip.audioFormat.sampleSizeInBits
            this.sampleRate = clip.audioFormat.sampleRate.toInt()
            this.openALFormat = findOpenALFormat(channels, bitsPerSample)

            when (clip.type) {
                AudioClip.Type.ALL_AT_ONCE -> {
                    val b = clip.getFullAudioData()
                    val data = b.data.toByteBuffer()
                    val alBuffer = this.queuedBuffers.first()
                    alBufferData(alBuffer, this.openALFormat, data, this.sampleRate)
                    alGetSourcei(this.sourceID, AL_BUFFER, intArrayOf(alBuffer)).catchALError()
                }
                AudioClip.Type.STREAMING -> {
                    queuedBuffers.forEach {
                        val data = clip.getAudioData(bufferSize).data.toByteBuffer()
                        alBufferData(it, this.openALFormat, data, this.sampleRate).catchALError()
                        alSourceQueueBuffers(this.sourceID, it).catchALError()
                    }
                }
            }

            readyToPlay.set(true)
            alSourcePlay(this.sourceID).catchALError()
        }

        override fun _process() {
            if (this.hasNextClip.get()) {
                readyToPlay.set(false)
                if (this::currentClip.isInitialized)
                    this.currentClip.rewind()
                prepareNewClip()
                this.nextClip = null
                this.hasNextClip.set(false)
            }

            if (!this::currentClip.isInitialized || !this.readyToPlay.get() || this.sourceID <= -1) return

            if (this.playRequested.get()) {
                this.playRequested.set(false)
                alSourcePlay(sourceID).catchALError()
            }

            if (this.pauseRequested.get()) {
                alSourcePause(sourceID).catchALError()
                return
            }
            if (this.stopRequested.get()) {
                alSourceStop(sourceID).catchALError()
                currentClip.rewind()
                return
            }

            if (!this.isPlaying) return

            alSourcef(sourceID, AL_GAIN, this.gain)
            alSourcef(sourceID, AL_PITCH, this.pitch)

            val validatedClip = this.currentClip.assertType<AudioClip>()
            if (validatedClip.type == AudioClip.Type.ALL_AT_ONCE) return

            val pBuffersProcessed = IntArray(1) { 0 }
            alGetSourcei(sourceID, AL_BUFFERS_PROCESSED, pBuffersProcessed).catchALError()
            val buffersProcessed = pBuffersProcessed.first()

            if (buffersProcessed <= 0) return

            for (i in 0 until buffersProcessed) {
                val pUnqueuedBuffer = IntArray(1) { 0 }
                alSourceUnqueueBuffers(sourceID, pUnqueuedBuffer).catchALError()
                val unqueuedBuffer = pUnqueuedBuffer.first()

                if (validatedClip.isEndOfStream) {
                    if (doLooping) {
                        validatedClip.rewind()
                    } else {
                        readyToPlay.set(false)
                        break
                    }
                }
                val audioBuffer = validatedClip.getAudioData(this.bufferSize)
                val alData = audioBuffer.data.toByteBuffer()

                alBufferData(unqueuedBuffer, this.openALFormat, alData, this.sampleRate).catchALError()
                alSourceQueueBuffers(sourceID, unqueuedBuffer).catchALError()
            }
        }

        override fun _shutdown() {
            alDeleteSources(sourceID)
            sourceID = -1

            queuedBuffers.forEach { alDeleteBuffers(it) }
        }
    }
}