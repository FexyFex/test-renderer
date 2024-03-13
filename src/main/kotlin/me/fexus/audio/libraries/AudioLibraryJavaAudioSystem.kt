package me.fexus.audio.libraries

import me.fexus.audio.*
import me.fexus.audio.AudioLibrary.Companion.assertType
import me.fexus.math.vec.Vec3
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine


@Suppress("unused")
class AudioLibraryJavaAudioSystem: AudioLibrary {
    override val libraryName: String = "Java AudioSystem"
    override val libraryDescription: String = "The default Java audio library."

    override var isInitialized: Boolean = false; private set

    override lateinit var listenerData: ListenerData


    override fun init() {
        isInitialized = true
    }

    override fun createClip(channelType: AudioClip.Type, decoder: AudioDataDecoder): AudioClip {
        assertInitialized()
        return AudioClip(channelType, decoder)
    }

    override fun createEmitter(): Emitter {
        assertInitialized()
        return Emitter()
    }

    override fun setListenerPosition(position: Vec3) {}
    override fun setListenerVelocity(velocity: Vec3) {}
    override fun setListenerOrientation(up: Vec3, lookAt: Vec3) {}

    override fun shutdown() {
        isInitialized = false
    }

    class Emitter: SoundEmitter {
        override var doLooping: Boolean = false
        override var gain: Float = 1f
        override var pitch: Float = 1f

        override val isPlaying: Boolean; get() = sourceDataLine.isActive

        override lateinit var currentClip: AudioClip
        private val hasNextClip = AtomicBoolean(false)
        private var nextClip: AudioClip? = null
        override var bufferSize: Int = FexAudioSystem.DEFAULT_BUFFER_SIZE
        private val queuedBuffers = mutableListOf<AudioBuffer>()

        private lateinit var sourceDataLine: SourceDataLine
        override val readyToPlay: AtomicBoolean = AtomicBoolean(false)


        override fun setVelocity(velocity: Vec3) {
            TODO("Not yet implemented")
        }

        override fun setPosition(position: Vec3) {
            TODO("Not yet implemented")
        }

        override fun play() {

        }

        override fun play(clip: AudioClip) {
            if (this.hasNextClip.get()) return
            this.nextClip = clip
            this.hasNextClip.set(true)
        }

        private fun prepareNewClip() {
            val validatedClip = this.nextClip!!

            if (!this::currentClip.isInitialized || validatedClip != currentClip)
                this.currentClip = validatedClip

            if (!this::sourceDataLine.isInitialized) {
                val lineInfo = DataLine.Info(SourceDataLine::class.java, validatedClip.audioFormat)
                this.sourceDataLine = AudioSystem.getLine(lineInfo) as SourceDataLine
            } else {
                this.sourceDataLine.stop()
                this.sourceDataLine.flush()
                this.sourceDataLine.close()
            }

            this.sourceDataLine.open(validatedClip.audioFormat)
            this.sourceDataLine.start()

            when (validatedClip.type) {
                AudioClip.Type.ALL_AT_ONCE -> {
                    val audioBuffer = validatedClip.getFullAudioData()
                    this.sourceDataLine.write(audioBuffer.data, 0, audioBuffer.data.size)
                }
                AudioClip.Type.STREAMING -> {
                    queuedBuffers.clear()
                    repeat(3) {
                        val buf = validatedClip.getAudioData(this.bufferSize)
                        this.queuedBuffers.add(it, buf)
                    }
                }
            }

            readyToPlay.set(true)
        }

        override fun pause() {
            this.readyToPlay.set(false)
        }

        override fun stop() {
            this.readyToPlay.set(false)
            if (this::currentClip.isInitialized)
                this.currentClip.rewind()
        }

        override fun _process() {
            if (this.hasNextClip.get()) {
                stop()
                prepareNewClip()
                this.hasNextClip.set(false)
                this.nextClip = null
            }

            if (!this::currentClip.isInitialized || !this::sourceDataLine.isInitialized || !readyToPlay.get()) return

            val validatedChannel = this.currentClip.assertType<AudioClip>()
            if (validatedChannel.type == AudioClip.Type.ALL_AT_ONCE) return
            if (this.queuedBuffers.isEmpty()) return

            val buf: AudioBuffer = queuedBuffers.removeAt(0)

            sourceDataLine.write(buf.data, 0, buf.data.size)

            if (!validatedChannel.isEndOfStream) {
                queuedBuffers.add(2, validatedChannel.getAudioData(this.bufferSize))
            } else {
                if (doLooping) {
                    validatedChannel.rewind()
                    queuedBuffers.add(2, validatedChannel.getAudioData(this.bufferSize))
                } else if (queuedBuffers.isEmpty()) this.readyToPlay.set(false)
            }
        }

        override fun _shutdown() {
            sourceDataLine.close()
        }
    }
}