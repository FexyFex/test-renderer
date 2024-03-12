package me.fexus.audio

import me.fexus.math.vec.Vec3
import kotlin.reflect.KClass


// Classes that implement this interface will be "interfaces" (not in a java way)
// to I guess what you could reasonably call an audio driver.
interface AudioLibrary {
    val libraryName: String
    val libraryDescription: String

    val isInitialized: Boolean
    var listenerData: ListenerData


    fun init()

    fun setListenerPosition(position: Vec3)
    fun setListenerRotation(up: Vec3, rotation: Vec3)
    fun setListenerVelocity(velocity: Vec3)

    fun createClip(channelType: AudioClip.Type, decoder: AudioDataDecoder): AudioClip

    fun createEmitter(): SoundEmitter

    fun shutdown()


    fun assertInitialized() {
        if (!this.isInitialized) throw LibraryUninitializedException(this.libraryName)
    }

    companion object {
        inline fun <reified E> Any.assertType(): E {
            if (this is E) return this
            else throw LibraryComponentMismatchExcpetion(E::class, this::class)
        }
    }

    class LibraryUninitializedException(libraryName: String): Exception(libraryName)
    class LibraryComponentMismatchExcpetion(expectedType: KClass<*>, actualyType: KClass<*>):
        Exception("Expected type is ${expectedType.qualifiedName} but got ${actualyType.qualifiedName}")
}