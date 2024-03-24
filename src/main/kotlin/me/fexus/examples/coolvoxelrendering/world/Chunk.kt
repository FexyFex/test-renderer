package me.fexus.examples.coolvoxelrendering.world

import me.fexus.voxel.SparseVoxelOctree
import me.fexus.voxel.VoxelOctree


class Chunk(private val svo: SparseVoxelOctree): VoxelOctree by svo {
    constructor(): this(SparseVoxelOctree())

    var sideInstanceCount = -1
    var offsetInPositionsBuffer = -1



}