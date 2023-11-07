package me.fexus.examples.hardwarevoxelraytracing.octree.buffer

import me.fexus.examples.hardwarevoxelraytracing.octree.IOctreeParentNode
import me.fexus.examples.hardwarevoxelraytracing.octree.OctreeRootNode
import me.fexus.examples.hardwarevoxelraytracing.voxel.type.VoidVoxel
import java.nio.ByteBuffer
import java.nio.ByteOrder


class OctreeBufferWriter(private val octree: OctreeRootNode) {
    fun write(): ByteBuffer {
        val leafNodeCount = getLeafNodeCount(octree)
        val parentNodeCount = getParentNodeCount(octree)
        val bufferSize = (leafNodeCount * LeafNodeEntry.SIZE_BYTES) + (parentNodeCount * ParentNodeEntry.SIZE_BYTES)
        val byteBuffer = ByteBuffer.allocate(bufferSize)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        writeToBuffer(byteBuffer, 0)

        return byteBuffer
    }

    fun writeToBuffer(byteBuffer: ByteBuffer, offset: Int) {
        val nodeEntries = mutableListOf<INodeEntry>()



        val rootEntry = ParentNodeEntry(VoxelData(octree.nodeData.voxelType), octree.childCount, )
    }


    private fun getLeafNodeCount(parentNode: IOctreeParentNode): Int {
        return parentNode.children.sumOf {
            if (it == null) 0 else if (it is IOctreeParentNode) getLeafNodeCount(it) else 1
        }
    }

    private fun getParentNodeCount(parentNode: IOctreeParentNode): Int {
        return parentNode.children.sumOf {
            if (it == null) 0 else if (it is IOctreeParentNode) getParentNodeCount(it) + 1 else 0
        }
    }
}