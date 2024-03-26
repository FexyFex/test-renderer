package me.fexus.examples.coolvoxelrendering.world

import me.fexus.math.vec.IVec3

enum class Direction(val value: Int, val normal: IVec3, val sidePositionOffset: IVec3) {
    RIGHT (0, IVec3(1, 0, 0), IVec3(1, 0, 0)),
    TOP   (1, IVec3(0, 1, 0), IVec3(0, 1, 0)),
    FRONT (2, IVec3(0, 0, 1), IVec3(0, 0, 1)),
    LEFT  (3, IVec3(-1, 0, 0), IVec3(0)),
    BOTTOM(4, IVec3(0, -1, 0), IVec3(0)),
    BACK  (5, IVec3(0, 0, -1), IVec3(0));

    companion object {
        val DIRECTIONS = Direction.values()
    }
}