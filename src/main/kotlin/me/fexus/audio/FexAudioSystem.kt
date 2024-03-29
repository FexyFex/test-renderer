package me.fexus.audio

import me.fexus.audio.command.*
import me.fexus.math.vec.Vec3
import java.io.InputStream


class FexAudioSystem {
    var isInitialized: Boolean = false

    lateinit var playbackThread: AudioCommmandThread


    inline fun <reified L: AudioLibrary> initWithLibrary(): FexAudioSystem {
        if (this.isInitialized) throw Exception("Already Initialized")

        this.playbackThread = AudioCommmandThread()
        this.playbackThread.start()

        val cmd = CommandInitializeLibrary(L::class)
        this.playbackThread.submitCommand(cmd)
        while (!cmd.isExecuted.get()) {}
        if (!cmd.result) throw Exception("Failed to initialize library")

        this.isInitialized = true

        return this
    }

    // Clips don't have any library-dependent logic, so it's fine to do it here
    fun createClip(type: AudioClip.Type, decoder: AudioDataDecoder): AudioClip {
        return AudioClip(type, decoder)
    }

    fun createEmitter(): SoundEmitter {
        val command = CommandCreateEmitter()
        playbackThread.submitCommand(command)
        while (!command.isExecuted.get()) {}
        return command.result
    }

    fun setListenerPosition(position: Vec3) = playbackThread.setListenerPosition(position)
    fun setListenerVelocity(velocity: Vec3) = playbackThread.setListenerVelocity(velocity)
    fun setListenerOrientation(lookingAt: Vec3, up: Vec3) = playbackThread.setListenerOrientation(lookingAt, up)
    fun setListenerData(listenerData: ListenerData) = playbackThread.setListenerData(listenerData)

    fun shutdown() {
        val cmd = CommandShutdown()
        playbackThread.submitCommand(cmd)
        this.isInitialized = false
    }


    companion object {
        const val DEFAULT_BUFFER_SIZE = 4096

        fun createDecoder(fileData: ByteArray, fileFormat: AudioFileFormat) = createDecoder(fileData.inputStream(), fileFormat)
        fun createDecoder(fileDataStream: InputStream, fileFormat: AudioFileFormat): AudioDataDecoder {
            val constructor = fileFormat.decoder.constructors.first()
            val decoder = constructor.call(fileDataStream) as AudioDataDecoder
            decoder.init()
            return decoder
        }
    }
}