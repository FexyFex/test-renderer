package me.fexus.examples.compute.bulletlimbo.level

import me.fexus.examples.compute.bulletlimbo.level.event.TimelineEvent
import java.nio.ByteBuffer


class EventTimeline(var events: MutableList<TimelineEvent> = mutableListOf()) {

    fun intoByteBuffer(buf: ByteBuffer, offset: Int) {
        var currentOffset = offset

        events.forEach {

            currentOffset += it.sizeBytes
        }
    }
}