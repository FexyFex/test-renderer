package me.fexus.voxel

import me.fexus.math.vec.IVec3
import me.fexus.voxel.type.VoxelType
import kotlin.math.log2
import kotlin.math.roundToInt


interface VoxelOctree {
    fun setVoxelAt(x: Int, y: Int, z: Int, voxelType: VoxelType)
    fun setVoxelAt(pos: IVec3, voxelType: VoxelType)

    fun getVoxelAt(x: Int, y: Int, z: Int, maxMipLevel: Int = MAX_MIP_LEVEL): VoxelType
    fun getVoxelAt(pos: IVec3, maxMipLevel: Int = MAX_MIP_LEVEL): VoxelType

    fun forEachVoxel(maxMipLevel: Int, action: (position: IVec3, voxel: VoxelType) -> Unit)

    companion object {
        const val EXTENT = 16
        const val VOXEL_COUNT = EXTENT * EXTENT * EXTENT
        val BOUNDS = 0 until EXTENT
        val MAX_MIP_LEVEL = log2(EXTENT.toFloat()).roundToInt() - 1
    }
}