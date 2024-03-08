package me.fexus.audio

import me.fexus.math.vec.DVec3
import me.fexus.math.vec.Vec3


interface SoundEmitter {
    val position: DVec3
    val velocity: Vec3

    var gain: Float
    var volume: Float
    var pitch: Float
    var doLooping: Boolean

    val isPlaying: Boolean

    val queuedBuffers: List<AudioBuffer>


    fun play(channel: AudioChannel)
}