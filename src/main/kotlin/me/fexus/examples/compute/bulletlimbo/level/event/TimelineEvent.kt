package me.fexus.examples.compute.bulletlimbo.level.event

import java.nio.ByteBuffer

interface TimelineEvent {
    val timeStamp: Double
    val sizeBytes: Int

    fun intoByteBuffer(buf: ByteBuffer, offset: Int)
}