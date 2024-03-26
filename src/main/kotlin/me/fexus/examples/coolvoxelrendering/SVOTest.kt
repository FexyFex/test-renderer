package me.fexus.examples.coolvoxelrendering

import me.fexus.examples.coolvoxelrendering.world.Chunk
import me.fexus.math.repeatCubed
import me.fexus.math.vec.IVec3
import me.fexus.voxel.VoxelOctree


fun main() {
    val svo = Chunk(IVec3(0))

    repeatCubed(6) { x, y, z ->
        svo.setVoxelAt(IVec3(x, y, z) * 2, 1)
    }

    println(svo.getVoxelAt(2,2,2))


    println("----------------")

    var count = 0
    svo.forEachVoxel(VoxelOctree.MAX_DEPTH) { position, voxel ->
        println("Voxel $voxel at position $position")
        count ++
    }

    println(count)
}