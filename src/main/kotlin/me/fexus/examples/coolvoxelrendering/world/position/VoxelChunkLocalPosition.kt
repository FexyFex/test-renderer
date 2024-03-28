package me.fexus.examples.coolvoxelrendering.world.position

import me.fexus.examples.coolvoxelrendering.world.Chunk
import me.fexus.math.vec.IVec3
import me.fexus.math.vec.TVec3


class VoxelChunkLocalPosition(x: Int, y: Int, z: Int): IVec3(x,y,z) {
    constructor(pos: IVec3): this(pos.x, pos.y, pos.z)


    fun toVoxelGlobalPosition(chunk: Chunk): VoxelGlobalPosition {
        return VoxelGlobalPosition(chunk.position.toVoxelGlobalsPosition() + this)
    }
}