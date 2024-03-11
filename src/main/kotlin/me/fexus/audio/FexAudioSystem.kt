package me.fexus.audio

import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean


class FexAudioSystem {
    lateinit var library: AudioLibrary

    lateinit var playbackThread: AudioPlaybackThread

    val isRunning = AtomicBoolean(false)


    inline fun <reified L: AudioLibrary> initWithLibrary(): FexAudioSystem {
        this.library = L::class.constructors.first().call()
        this.library.init()

        isRunning.set(true)

        this.playbackThread = AudioPlaybackThread(isRunning)
        this.playbackThread.start()

        return this
    }

    fun createDecoder(fileData: ByteArray, fileFormat: AudioFileFormat) = createDecoder(fileData.inputStream(), fileFormat)
    fun createDecoder(fileDataStream: InputStream, fileFormat: AudioFileFormat): AudioDataDecoder {
        val constructor = fileFormat.decoder.constructors.first { it.parameters.first().name == "audioStream" }
        val decoder = constructor.call(fileDataStream) as AudioDataDecoder
        decoder.init()
        return decoder
    }

    fun createEmitter() = library.createEmitter()
    fun createChannel(channelType: AudioChannel.Type, decoder: AudioDataDecoder) =
        library.createChannel(channelType, decoder)

    fun shutdown() {
        isRunning.set(false)
    }


    companion object {
        const val BUFFER_SIZE = 2048
    }
}