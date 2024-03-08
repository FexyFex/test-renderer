package me.fexus.audio


class FexAudioSystem {
    lateinit var library: AudioLibrary

    val isInitialized: Boolean; get() = this::library.isInitialized && library.isInitialized


    inline fun <reified L: AudioLibrary> initWithLibrary(): FexAudioSystem {
        this.library = L::class.constructors.first().call()
        this.library.init()

        return this
    }


    fun createEmitter() = library.createEmitter()
    fun createChannel(channelType: AudioChannel.Type) = library.createChannel(channelType)


    fun close() {

    }
}