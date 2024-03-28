package me.fexus.examples.coolvoxelrendering.world

import me.fexus.math.vec.IVec3
import me.fexus.voxel.VoxelOctree


class Chunk(val position: IVec3, private val svo: SparseVoxelOctree): VoxelOctree<Int> by svo {
    constructor(position: IVec3): this(position, SparseVoxelOctree())

    var isFull: Boolean = false
    var isEmpty: Boolean = true


    companion object {
        const val EXTENT = VoxelOctree.EXTENT
        const val VOXELS_PER_CHUNK = EXTENT * EXTENT * EXTENT
    }
}