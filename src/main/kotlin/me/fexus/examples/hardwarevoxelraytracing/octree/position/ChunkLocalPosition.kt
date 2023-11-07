package me.fexus.examples.hardwarevoxelraytracing.octree.position

// All params will lie between 0 (inclusive) and Chunk.EXTENT (non-inclusive)
data class ChunkLocalPosition(override val x: Int, override val y: Int, override val z: Int): IOctreePosition
