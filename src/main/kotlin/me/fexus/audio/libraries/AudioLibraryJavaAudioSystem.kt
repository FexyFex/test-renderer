package me.fexus.audio.libraries

import me.fexus.audio.*
import me.fexus.audio.AudioLibrary.Companion.assertType
import me.fexus.math.vec.DVec3
import me.fexus.math.vec.Vec3
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine


class AudioLibraryJavaAudioSystem: AudioLibrary {
    override val libraryName: String = "Java AudioSystem"
    override val libraryDescription: String = "The default Java audio library."

    override var isInitialized: Boolean = false; private set

    override lateinit var listenerData: ListenerData


    override fun init() {
        isInitialized = true
    }

    override fun createChannel(channelType: AudioChannel.Type, decoder: AudioDataDecoder): Channel {
        assertInitialized()
        return Channel(channelType, decoder)
    }

    override fun createEmitter(): Emitter {
        assertInitialized()
        return Emitter()
    }

    override fun shutdown() {
        isInitialized = false
    }

    class Channel(override val type: AudioChannel.Type, override val decoder: AudioDataDecoder): AudioChannel, AudioDataDecoder by decoder {
        override val timestamp: Float; get() = -1f

        override fun prepareBuffers(buffers: List<AudioBuffer>) {}
        override fun queueBuffer(buffer: AudioBuffer) {}

        override fun clear() {}
    }

    class Emitter: SoundEmitter {
        override val position: DVec3 = DVec3(0.0)
        override val velocity: Vec3 = Vec3(0f)

        override var doLooping: Boolean = false
        override var volume: Float = 1f
        override var gain: Float = 1f
        override val isPlaying: Boolean; get() = sourceDataLine.isActive
        override var pitch: Float = 1f

        override lateinit var queuedBuffers: List<AudioBuffer>
        private lateinit var currentChannel: Channel

        private lateinit var sourceDataLine: SourceDataLine


        override fun play(channel: AudioChannel) {
            val validatedChannel: Channel = channel.assertType<Channel>()
            if (!this::currentChannel.isInitialized || validatedChannel != currentChannel)
                this.currentChannel = validatedChannel

            if (!this::sourceDataLine.isInitialized) {
                val lineInfo = DataLine.Info(SourceDataLine::class.java, validatedChannel.audioFormat)
                this.sourceDataLine = AudioSystem.getLine(lineInfo) as SourceDataLine
            } else {
                this.sourceDataLine.stop()
                this.sourceDataLine.flush()
                this.sourceDataLine.close()
            }

            this.sourceDataLine.open(validatedChannel.audioFormat)
            this.sourceDataLine.start()

            if (validatedChannel.type == AudioChannel.Type.ALL_AT_ONCE) {
                val audioBuffer = validatedChannel.getFullAudioData()
                this.sourceDataLine.write(audioBuffer.data, 0, audioBuffer.data.size)
            }
        }
    }
}