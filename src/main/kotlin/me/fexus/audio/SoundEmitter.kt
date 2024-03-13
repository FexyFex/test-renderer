package me.fexus.audio


import me.fexus.math.vec.Vec3
import java.util.concurrent.atomic.AtomicBoolean


interface SoundEmitter {
    var gain: Float
    var pitch: Float
    var doLooping: Boolean

    val isPlaying: Boolean

    var currentClip: AudioClip
    val bufferSize: Int
    val readyToPlay: AtomicBoolean


    fun play(clip: AudioClip)
    fun play()
    fun pause()
    fun stop()

    fun setPosition(position: Vec3)
    fun setVelocity(velocity: Vec3)

    fun destroy() {}

    fun _process()

    fun _shutdown()
}