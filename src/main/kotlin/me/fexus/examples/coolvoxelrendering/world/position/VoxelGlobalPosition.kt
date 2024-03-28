package me.fexus.examples.coolvoxelrendering.world.position

import me.fexus.examples.coolvoxelrendering.world.Chunk
import me.fexus.math.vec.IVec3
import kotlin.math.ceil


class VoxelGlobalPosition(x: Int, y: Int, z: Int): IVec3(x, y, z) {
    constructor(pos: IVec3): this(pos.x, pos.y, pos.z)


    fun toChunkPosition(): ChunkPosition {
        return ChunkPosition(
            ceil(x.toFloat() / Chunk.EXTENT).toInt() - if (x < 0) 1 else 0,
            ceil(y.toFloat() / Chunk.EXTENT).toInt() - if (y < 0) 1 else 0,
            ceil(z.toFloat() / Chunk.EXTENT).toInt() - if (z < 0) 1 else 0,
        )
    }

    fun toChunkLocalPos(): VoxelChunkLocalPosition {
        return VoxelChunkLocalPosition(
            x % Chunk.EXTENT + if (x < 0) Chunk.EXTENT else 0,
            y % Chunk.EXTENT + if (y < 0) Chunk.EXTENT else 0,
            z % Chunk.EXTENT + if (z < 0) Chunk.EXTENT else 0,
        )
    }
}