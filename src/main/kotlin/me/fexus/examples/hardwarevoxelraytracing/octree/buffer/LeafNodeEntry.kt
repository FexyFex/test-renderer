package me.fexus.examples.hardwarevoxelraytracing.octree.buffer

import java.nio.ByteBuffer

data class LeafNodeEntry(override val voxelData: VoxelData): INodeEntry {
    override val typeSignature: NodeTypeSignature = NodeTypeSignature.LEAF

    override fun toByteBuffer(buffer: ByteBuffer, offset: Int) {
        buffer.put(offset, typeSignature.value)
        voxelData.toByteBuffer(buffer, offset + 1)
    }

    companion object {
        const val SIZE_BYTES = VoxelData.SIZE_BYTES + 1
    }
}
