package me.fexus.examples.hardwarevoxelraytracing

import me.fexus.voxel.VoxelRegistry
import me.fexus.voxel.type.CoalVoxel
import me.fexus.voxel.SparseVoxelOctree
import me.fexus.math.repeatCubed
import me.fexus.voxel.octree.buffer.buildSVOBuffer
import me.fexus.voxel.octree.buffer.createIndexedOctree
import me.fexus.voxel.octree.buffer.createIndexedOctreeNodeList
import kotlin.system.measureNanoTime


fun main() {
    VoxelRegistry.init()

    repeat(500) {
        val chunk = SparseVoxelOctree()

        repeatCubed(SparseVoxelOctree.EXTENT) { x, y, z ->
            if (Math.random() > 0.5)
                chunk.insertIntoOctree(x, y, z, CoalVoxel)
        }

        val time = measureNanoTime {
            val indexedOctree = createIndexedOctree(chunk.octree, 0).node
            val nodeList = createIndexedOctreeNodeList(indexedOctree)

            val buf = buildSVOBuffer {
                nodeList.forEach { append(it) }
            }
        }
        println("Time: $time")
    }
}
