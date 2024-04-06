package me.fexus.examples.coolvoxelrendering.world.position

import me.fexus.examples.coolvoxelrendering.world.Chunk
import me.fexus.math.vec.DVec3
import me.fexus.math.vec.TVec3
import me.fexus.math.vec.Vec3
import kotlin.math.roundToInt


class WorldPosition(x: Double, y: Double, z: Double): DVec3(x, y, z) {
    constructor(vec: DVec3): this(vec.x, vec.y, vec.z)


    fun toVoxelWorldPosition(): VoxelWorldPosition {
        return VoxelWorldPosition(x.roundToInt(), y.roundToInt(), z.roundToInt())
    }

    fun toChunkPosition(): ChunkPosition {
        return ChunkPosition(
            (x / Chunk.EXTENT).roundToInt() - if (x < 0 || x % Chunk.EXTENT == 0.0) 1 else 0,
            (y / Chunk.EXTENT).roundToInt() - if (y < 0 || y % Chunk.EXTENT == 0.0) 1 else 0,
            (z / Chunk.EXTENT).roundToInt() - if (z < 0 || z % Chunk.EXTENT == 0.0) 1 else 0,
        )
    }

    fun toChunkLocalPositon(): ChunkLocalPosition {
        val modded = Vec3(x % Chunk.EXTENT, y % Chunk.EXTENT, z % Chunk.EXTENT)
        return ChunkLocalPosition(modded + Chunk.EXTENT)
    }


    operator fun plusAssign(other: Vec3) {
        this.x += other.x
        this.y += other.y
        this.z += other.z
    }
}