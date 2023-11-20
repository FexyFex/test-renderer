package me.fexus.examples.hardwarevoxelraytracing

import me.fexus.octree.compression.dag.OctreeCompressorDAG
import me.fexus.examples.hardwarevoxelraytracing.voxel.VoxelRegistry
import me.fexus.examples.hardwarevoxelraytracing.voxel.type.CoalVoxel
import me.fexus.examples.hardwarevoxelraytracing.voxel.type.StoneVoxel
import me.fexus.examples.hardwarevoxelraytracing.world.SparseVoxelOctree
import me.fexus.math.repeatSquared


fun main() {
    VoxelRegistry.init()

    val chunk = SparseVoxelOctree()
    repeatSquared(SparseVoxelOctree.EXTENT) { x, y ->
        chunk.insertIntoOctree(x,y,0, StoneVoxel)
    }
    chunk.insertIntoOctree(1,1,1, CoalVoxel)


    val bufferWriter = OctreeCompressorDAG(chunk.octree)
    val buf = bufferWriter.createDAG()
}
