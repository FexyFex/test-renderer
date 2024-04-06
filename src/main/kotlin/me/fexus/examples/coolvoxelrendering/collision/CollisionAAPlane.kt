package me.fexus.examples.coolvoxelrendering.collision

import me.fexus.math.vec.DVec3
import me.fexus.math.vec.Vec3


class CollisionAAPlane(val position: DVec3, val extent: Vec3, val normal: Vec3) {

    fun getPoints(): Array<DVec3> {
        val min = position
        val max = position + extent

        val p3 = DVec3(position)
        val p4 = DVec3(position)
        when {
            normal.x != 0f -> {
                p3.y += extent.y
                p4.z += extent.z
            }
            normal.y != 0f -> {
                p3.x += extent.x
                p4.z += extent.z
            }
            normal.z != 0f -> {
                p3.x += extent.x
                p4.y += extent.y
            }
        }

        return arrayOf(min, p3, p4, max)
    }
}