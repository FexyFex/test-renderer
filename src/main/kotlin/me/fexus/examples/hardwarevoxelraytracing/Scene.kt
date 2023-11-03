package me.fexus.examples.hardwarevoxelraytracing

import me.fexus.examples.hardwarevoxelraytracing.accelerationstructure.AABB

object Scene {
    val aabbs = listOf<AABB>(
        AABB(0f, 0f, 0f, 1f, 1f, 1f),
        AABB(1f, 1f, 1f, 2f, 2f, 2f),
    )
}