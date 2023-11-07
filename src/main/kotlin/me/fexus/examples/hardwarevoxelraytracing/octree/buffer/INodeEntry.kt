package me.fexus.examples.hardwarevoxelraytracing.octree.buffer

import java.nio.ByteBuffer


interface INodeEntry {
    val typeSignature: NodeTypeSignature
    val voxelData: VoxelData

    fun toByteBuffer(buffer: ByteBuffer, offset: Int)
}