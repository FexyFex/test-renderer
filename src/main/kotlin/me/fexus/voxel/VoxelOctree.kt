package me.fexus.voxel

import me.fexus.math.vec.IVec3
import me.fexus.voxel.type.VoxelType
import kotlin.math.log2
import kotlin.math.roundToInt


interface VoxelOctree<T> {
    fun setVoxelAt(x: Int, y: Int, z: Int, value: T)
    fun setVoxelAt(pos: IVec3, value: T)

    fun getVoxelAt(x: Int, y: Int, z: Int, maxMipLevel: Int = MAX_DEPTH): T
    fun getVoxelAt(pos: IVec3, maxMipLevel: Int = MAX_DEPTH): T

    fun forEachVoxel(maxDepth: Int, action: (position: IVec3, voxel: T) -> Unit)

    companion object {
        const val EXTENT = 16
        const val VOXEL_COUNT = EXTENT * EXTENT * EXTENT
        val BOUNDS = 0 until EXTENT
        val MAX_DEPTH = log2(EXTENT.toFloat()).roundToInt() - 1
    }
}