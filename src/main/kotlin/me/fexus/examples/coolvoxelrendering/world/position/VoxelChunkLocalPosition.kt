package me.fexus.examples.coolvoxelrendering.world.position

import me.fexus.examples.coolvoxelrendering.world.Chunk
import me.fexus.math.vec.IVec3


class VoxelChunkLocalPosition(x: Int, y: Int, z: Int): IVec3(x,y,z) {
    constructor(pos: IVec3): this(pos.x, pos.y, pos.z)


    fun toVoxelGlobalPosition(chunk: Chunk): VoxelWorldPosition {
        return VoxelWorldPosition(chunk.position.toVoxelGlobalsPosition() + this)
    }
}