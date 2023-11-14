package me.fexus.octree.compression

import me.fexus.octree.IOctreeNodeData
import me.fexus.octree.IOctreeParentNode
import me.fexus.octree.OctreeNodeDataVoxelType
import me.fexus.octree.OctreeRootNode
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.log2


class OctreeCompressor(private val octree: OctreeRootNode<OctreeNodeDataVoxelType>) {
    fun createSVO(): SVOSet {
        val indexedOctree = createIndexedOctree(octree, 0).node
        val indexedNodes = getNodeList(indexedOctree)
        val uniqueNodeData = indexedNodes.distinctBy { it.nodeData }.map { it.nodeData }
        val bitsPerIndex = ceil(log2(uniqueNodeData.size.toFloat())).toInt()
        val svoNodes = createSVONodeList(indexedOctree)

        val textureIndexBufferSize = uniqueNodeData.size * Int.SIZE_BYTES
        val textureIndexBuffer = ByteBuffer.allocate(textureIndexBufferSize)

        val indexBufferSize = ceil((bitsPerIndex * indexedNodes.size) / 8f).toInt()
        val indexBuffer = ByteBuffer.allocate(indexBufferSize)

        val nodeBufferSize = svoNodes.sumOf { Int.SIZE_BYTES + it.childPointers.size * Int.SIZE_BYTES }
        val nodeBuffer = ByteBuffer.allocate(nodeBufferSize)

        return SVOSet(nodeBuffer, bitsPerIndex, indexBuffer, textureIndexBuffer)
    }

    private fun createSVONodeList(indexedOctree: IndexedParentNode): List<SVONode> {
        val svoNodes = mutableListOf<SVONode>()

        fun rec(targetNode: IIndexedOctreeNode) {
            val childPointers = mutableListOf<SVOChildNodePointer>()
            if (targetNode is IndexedParentNode) {
                targetNode.children.forEachIndexed { octantIndex, childNode ->
                    if (childNode == null) return@forEachIndexed
                    val pointer = SVOChildNodePointer(octantIndex, childNode.index - targetNode.index, childNode.index)
                    childPointers.add(pointer)
                    rec(childNode)
                }
            }
            val svoNode = SVONode(targetNode.index, childPointers.size, childPointers.toTypedArray())
            svoNodes.add(svoNode)
        }

        rec(indexedOctree)
        return svoNodes.sortedBy { it.index }
    }

    data class LastIndexAndNode(val lastIndex: Int, val node: IndexedParentNode)
    private fun createIndexedOctree(parentNode: IOctreeParentNode<OctreeNodeDataVoxelType>, startingIndex: Int): LastIndexAndNode {
        var currentIndex = startingIndex

        val indexedNode = IndexedParentNode(currentIndex++, parentNode.nodeData)

        parentNode.children.forEachIndexed { index, childNode ->
            if (childNode == null) return@forEachIndexed
            if (childNode is IOctreeParentNode) {
                val packetThingy = createIndexedOctree(childNode, currentIndex++)
                indexedNode.children[index] = packetThingy.node
                currentIndex = packetThingy.lastIndex
            } else {
                indexedNode.children[index] = IndexedLeafNode(currentIndex++, childNode.nodeData)
            }
        }

        return LastIndexAndNode(currentIndex, indexedNode)
    }

    private fun getNodeList(rootNode: IIndexedOctreeNode): List<IIndexedOctreeNode> {
        val allNodes = mutableListOf<IIndexedOctreeNode>()
        allNodes.add(rootNode)
        var nodeIndex = 0

        while (nodeIndex < allNodes.size) {
            val node = allNodes[nodeIndex]
            if (node is IndexedParentNode) allNodes.addAll(node.children.filterNotNull())
            nodeIndex++
        }

        return allNodes
    }
}