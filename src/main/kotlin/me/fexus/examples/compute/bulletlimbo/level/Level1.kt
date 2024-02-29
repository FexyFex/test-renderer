package me.fexus.examples.compute.bulletlimbo.level

import me.fexus.examples.compute.bulletlimbo.enemy.type.Butterfly
import me.fexus.examples.compute.bulletlimbo.level.event.EnemySpawnEvent


object Level1: ILevel {
    override val eventTimeline: EventTimeline = EventTimeline(
        mutableListOf(EnemySpawnEvent(1.0, Butterfly))
    )
}