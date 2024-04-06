package me.fexus.examples.coolvoxelrendering.collision

import me.fexus.math.vec.Vec3


data class CollisionAABB2(val position: Vec3, val extent: Vec3) {
    val min: Vec3; get() = position
    val max: Vec3; get() = position + extent

    fun getPoints(): Array<Vec3> {
        val min = position
        val max = position + extent
        return arrayOf(
            Vec3(min.x, min.y, min.z),
            Vec3(max.x, min.y, min.z),
            Vec3(min.x, max.y, min.z),
            Vec3(min.x, min.y, max.z),

            Vec3(max.x, max.y, max.z),
            Vec3(min.x, max.y, max.z),
            Vec3(max.x, min.y, max.z),
            Vec3(max.x, max.y, min.z)
        )
    }
}