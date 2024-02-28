package me.fexus.examples.compute.bulletlimbo

import me.fexus.math.vec.Vec2


// Not really used CPU side. Just need the SIZE_BYTES value
data class BulletData(
    val position: Vec2,
    val rotation: Float,
    val lifetime: Float,
    val timeLived: Float,
    val visualID: Int,
    val behaviourID: Int,
    val behaviourData: Vec2
) {


    companion object {
        const val SIZE_BYTES = (Vec2.SIZE_BYTES * 2) + (Float.SIZE_BYTES * 3) + (Int.SIZE_BYTES * 2)
    }
}
