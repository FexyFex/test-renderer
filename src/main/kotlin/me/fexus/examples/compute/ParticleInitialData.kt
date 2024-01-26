package me.fexus.examples.compute

import me.fexus.math.vec.Vec2


data class ParticleInitialData(
        val spawnPosition: Vec2,
        val spawnTimeStamp: Float,
        val behaviourID: Int,
        val visualID: Int
) {
    companion object {
        const val SIZE_BYTES = 32
    }
}
