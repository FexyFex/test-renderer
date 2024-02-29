package me.fexus.examples.compute.bulletlimbo.level.event

import me.fexus.examples.compute.bulletlimbo.enemy.type.EnemyType

class EnemySpawnEvent(override val timeStamp: Double, val enemy: EnemyType): TimelineEvent {
    override val sizeBytes: Int = Float.SIZE_BYTES + Int.SIZE_BYTES
}