package me.fexus.examples.hardwarevoxelraytracing.octree.buffer

import java.nio.ByteBuffer


class ParentNodeEntry(override val voxelData: VoxelData, val childCount: Byte, val childOffsets: IntArray): INodeEntry {
    override val typeSignature: NodeTypeSignature = NodeTypeSignature.PARENT

    override fun toByteBuffer(buffer: ByteBuffer, offset: Int) {
        buffer.put(offset, typeSignature.value)
        voxelData.toByteBuffer(buffer, offset + 1)
        buffer.put(offset + 1 + VoxelData.SIZE_BYTES, childCount)
        childOffsets.forEachIndexed { index, childOffset ->
            val lOffset = offset + 2 + VoxelData.SIZE_BYTES + (index * Int.SIZE_BYTES)
            buffer.putInt(lOffset, childOffset)
        }
    }


    companion object {
        const val SIZE_BYTES: Int = (1 + VoxelData.SIZE_BYTES + 1 + (8 * Int.SIZE_BYTES))
    }
}