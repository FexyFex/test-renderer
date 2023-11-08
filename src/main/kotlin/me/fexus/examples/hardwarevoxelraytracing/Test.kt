package me.fexus.examples.hardwarevoxelraytracing

import me.fexus.examples.hardwarevoxelraytracing.octree.compression.OctreeCompressor
import me.fexus.examples.hardwarevoxelraytracing.voxel.VoxelRegistry
import me.fexus.examples.hardwarevoxelraytracing.voxel.type.CoalVoxel
import me.fexus.examples.hardwarevoxelraytracing.voxel.type.StoneVoxel
import me.fexus.examples.hardwarevoxelraytracing.world.Chunk
import me.fexus.math.repeatSquared


fun main() {
    VoxelRegistry.init()

    val chunk = Chunk()
    repeatSquared(Chunk.EXTENT) { x, y ->
        chunk.insertIntoOctree(x,y,0, StoneVoxel)
    }
    chunk.insertIntoOctree(1,1,3, CoalVoxel)


    val bufferWriter = OctreeCompressor(chunk.octree)
    val buf = bufferWriter.createSVO()
}
