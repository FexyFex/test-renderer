package me.fexus.examples.coolvoxelrendering

import me.fexus.examples.coolvoxelrendering.world.Chunk
import me.fexus.math.repeatCubed
import me.fexus.math.vec.IVec3
import me.fexus.voxel.VoxelOctree


fun main() {
    val svo = Chunk(IVec3(0))

    repeatCubed(4) { x, y, z ->
        svo.setVoxelAt(IVec3(x, y, z), 1)
    }
    repeatCubed(4) { x, y, z ->
        svo.setVoxelAt(IVec3(x, y, z) + 1, 0)
    }
    svo.setVoxelAt(1,1,1, 0)

    println("----------------")

    var count = 0
    svo.forEachVoxel(VoxelOctree.MAX_DEPTH) { position, voxel ->
        println(position)
        if (svo.getVoxelAt(position) != 1) println("MISTAKE AT $position")
        count ++
    }
    println(count)

    println("----------------")
}