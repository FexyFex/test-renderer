package me.fexus.examples.coolvoxelrendering.world

import me.fexus.math.vec.IVec3
import me.fexus.voxel.SparseVoxelOctree
import me.fexus.voxel.VoxelOctree


class Chunk(val position: IVec3, private val svo: SparseVoxelOctree): VoxelOctree by svo {
    constructor(position: IVec3): this(position, SparseVoxelOctree())


    companion object {
        const val EXTENT = VoxelOctree.EXTENT
        const val VOXELS_PER_CHUNK = EXTENT * EXTENT * EXTENT
    }
}