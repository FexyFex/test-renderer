package me.fexus.examples.coolvoxelrendering.world

import me.fexus.examples.coolvoxelrendering.world.position.ChunkPosition
import me.fexus.math.vec.IVec3
import me.fexus.voxel.VoxelOctree


class Chunk(val position: ChunkPosition, private val svo: SparseVoxelOctree): VoxelOctree<Int> by svo {
    constructor(position: IVec3): this(ChunkPosition(position), SparseVoxelOctree())

    var isFull: Boolean = false
    var isEmpty: Boolean = true
    var isSoilFlagged: Boolean = false


    companion object {
        const val EXTENT = VoxelOctree.EXTENT
        const val VOXELS_PER_CHUNK = EXTENT * EXTENT * EXTENT
    }
}