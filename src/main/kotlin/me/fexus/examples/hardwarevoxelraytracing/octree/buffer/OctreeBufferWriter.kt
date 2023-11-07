package me.fexus.examples.hardwarevoxelraytracing.octree.buffer

import me.fexus.examples.hardwarevoxelraytracing.octree.IOctreeNode
import me.fexus.examples.hardwarevoxelraytracing.octree.IOctreeParentNode
import me.fexus.examples.hardwarevoxelraytracing.octree.OctreeLeafNode
import me.fexus.examples.hardwarevoxelraytracing.octree.OctreeRootNode
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
        val nodes = mutableListOf<IOctreeNode>()

        nodes.add(octree)
        var currentNodeIndex = 0
        var nextFreeIndex = 1

        while (currentNodeIndex < nodes.size) {
            val node = nodes[currentNodeIndex]
            if (node is IOctreeParentNode) {
                val childIndices = mutableListOf<Int>()
                node.children.forEach {
                    if (it != null) {
                        nodes.add(it)
                        childIndices.add(nextFreeIndex)
                        nextFreeIndex++
                    }
                }
                val entry = ParentNodeEntry(VoxelData(node.nodeData.voxelType), node.childCount.toByte(), childIndices.toIntArray())
                nodeEntries.add(entry)
            }
            else if (node is OctreeLeafNode) {
                val entry = LeafNodeEntry(VoxelData(node.nodeData.voxelType))
                nodeEntries.add(entry)
            }
            currentNodeIndex++
        }

        //val rootEntry = ParentNodeEntry(VoxelData(octree.nodeData.voxelType), octree.childCount, )
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