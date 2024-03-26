package me.fexus.examples.coolvoxelrendering.world

import me.fexus.math.vec.IVec3
import me.fexus.voxel.SparseVoxelOctree
import me.fexus.voxel.VoxelOctree
import java.util.concurrent.atomic.AtomicBoolean


class Chunk(val position: IVec3, private val svo: SparseVoxelOctree): VoxelOctree by svo {
    constructor(position: IVec3): this(position, SparseVoxelOctree())

    var submittedForHulling: Boolean = false
    var isFull: Boolean = false


    companion object {
        const val EXTENT = VoxelOctree.EXTENT
        const val VOXELS_PER_CHUNK = EXTENT * EXTENT * EXTENT
    }
}