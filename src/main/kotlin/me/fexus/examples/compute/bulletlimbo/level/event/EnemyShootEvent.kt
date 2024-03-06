package me.fexus.examples.compute.bulletlimbo.level.event

import me.fexus.examples.compute.bulletlimbo.bullet.type.BulletType
import java.nio.ByteBuffer


class EnemyShootEvent(override val timeStamp: Double, val bullet: BulletType): TimelineEvent {
    override val sizeBytes: Int = Float.SIZE_BYTES + Int.SIZE_BYTES

    override fun intoByteBuffer(buf: ByteBuffer, offset: Int) {
        TODO("Not yet implemented")
    }
}