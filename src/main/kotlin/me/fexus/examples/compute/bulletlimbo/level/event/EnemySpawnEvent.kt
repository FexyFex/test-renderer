package me.fexus.examples.compute.bulletlimbo.level.event

import me.fexus.examples.compute.bulletlimbo.enemy.type.EnemyType
import java.nio.ByteBuffer

class EnemySpawnEvent(override val timeStamp: Double, val enemy: EnemyType): TimelineEvent {
    override val sizeBytes: Int = Float.SIZE_BYTES + Int.SIZE_BYTES

    override fun intoByteBuffer(buf: ByteBuffer, offset: Int) {
        TODO("Not yet implemented")
    }
}