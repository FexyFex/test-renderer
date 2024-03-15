package me.fexus.examples.surroundsound

import me.fexus.audio.AudioClip
import me.fexus.audio.FexAudioSystem
import me.fexus.examples.surroundsound.monolithflag.MonolithFlag
import me.fexus.examples.surroundsound.monolithflag.MonolithFlags
import me.fexus.math.mat.Mat4
import me.fexus.math.vec.Vec3
import java.nio.ByteBuffer


class HummingMonolith(val position: Vec3, val rotation: Vec3, private val audioSystem: FexAudioSystem, private val clip: AudioClip) {
    private val emitter = audioSystem.createEmitter()

    private var flags: MonolithFlags = MonolithFlag.NONE


    fun play() {
        emitter.play(clip)
        emitter.setPosition(position)
        flags += MonolithFlag.HUMMMING
    }

    fun stop() {
        emitter.stop()
        flags -= MonolithFlag.HUMMMING
    }


    fun intoByteBuffer(buf: ByteBuffer, offset: Int) {
        val matrix = Mat4().translate(position)
            .rotate(rotation.x, Vec3(1f, 0f, 0f))
            .rotate(rotation.y, Vec3(0f, 1f, 0f))
            .rotate(rotation.z, Vec3(0f, 0f, 1f))

        matrix.toByteBufferColumnMajor(buf, offset)
        buf.putInt(offset + Mat4.SIZE_BYTES, flags.bits)
    }


    companion object {
        const val SIZE_BYTES = 128
    }
}