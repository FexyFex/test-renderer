package me.fexus.examples.hardwarevoxelraytracing

import me.fexus.octree.compression.dag.OctreeCompressorDAG
import me.fexus.examples.hardwarevoxelraytracing.voxel.VoxelRegistry
import me.fexus.examples.hardwarevoxelraytracing.voxel.type.CoalVoxel
import me.fexus.examples.hardwarevoxelraytracing.voxel.type.StoneVoxel
import me.fexus.examples.hardwarevoxelraytracing.world.SparseVoxelOctree
import me.fexus.math.repeatCubed
import me.fexus.math.repeatSquared


fun main() {
    VoxelRegistry.init()

    repeat(500) {
        val chunk = SparseVoxelOctree()

        repeatCubed(SparseVoxelOctree.EXTENT) { x, y, z ->
            if (Math.random() > 0.5)
                chunk.insertIntoOctree(x, y, z, CoalVoxel)
        }

        val bufferWriter = OctreeCompressorDAG(chunk.octree)

        val buf = bufferWriter.createDAG()
    }
}
