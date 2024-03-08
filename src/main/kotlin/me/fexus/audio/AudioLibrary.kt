package me.fexus.audio

import kotlin.reflect.KClass


// Classes that implement this interface will be "interfaces" (not in a java way)
// to I guess what you could reasonably call an audio driver.
interface AudioLibrary {
    val libraryName: String
    val libraryDescription: String

    val isInitialized: Boolean
    val listenerData: ListenerData


    fun init()

    fun createChannel(channelType: AudioChannel.Type): AudioChannel

    fun createEmitter(): SoundEmitter

    fun shutdown()


    fun assertInitialized() {
        if (!this.isInitialized) throw LibraryUninitializedException(this.libraryName)
    }

    companion object {
        inline fun <reified E, reified G> G.assertType(expected: E): E {
            if (this is E) return this
            else throw LibraryComponentMismatchExcpetion(E::class, G::class)
        }
    }

    class LibraryUninitializedException(libraryName: String): Exception(libraryName)
    class LibraryComponentMismatchExcpetion(expectedType: KClass<*>, actualyType: KClass<*>):
        Exception("Expected type is ${expectedType.qualifiedName} but got ${actualyType.qualifiedName}")
}