package me.fexus.examples.hardwarevoxelraytracing.voxelanimation

import me.fexus.math.vec.IVec3

class VoxelGridSubRange(val xRange: IntRange, val yRange: IntRange, val zRange: IntRange) {
    constructor(min: IVec3, max: IVec3): this(IntRange(min.x, max.x), IntRange(min.y, max.y), IntRange(min.z, max.z))

    fun forEachVoxel(func: (x: Int, y: Int, z: Int) -> Unit) {
        zRange.forEach { z ->
            yRange.forEach { y ->
                xRange.forEach { x ->
                    func(x,y,z)
                }
            }
        }
    }
}