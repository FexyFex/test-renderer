package me.fexus.examples.compute.bulletlimbo

import me.fexus.math.vec.Vec2
import java.nio.ByteBuffer


data class Area2D(val center: Vec2, val extent: Vec2) {
    fun toByteBuffer(buf: ByteBuffer, offset: Int) {
        center.intoByteBuffer(buf, offset)
        extent.intoByteBuffer(buf, offset + Vec2.SIZE_BYTES)
    }
}
