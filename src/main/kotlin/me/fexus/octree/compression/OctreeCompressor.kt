package me.fexus.octree.compression

import me.fexus.octree.IOctreeNodeData
import me.fexus.octree.IOctreeParentNode
import me.fexus.octree.OctreeNodeDataVoxelType
import me.fexus.octree.OctreeRootNode
import java.nio.ByteBuffer
import java.util.concurrent.LinkedTransferQueue
import kotlin.math.ceil
import kotlin.math.log2


class OctreeCompressor(private val octree: OctreeRootNode<OctreeNodeDataVoxelType>) {
    fun createSVO(): SVOSet {
        val indexedOctree = createIndexedOctree(octree, 0).node
        val indexedNodes = getNodeList(indexedOctree)
        val uniqueNodeData = indexedNodes.distinctBy { it.nodeData }.map { it.nodeData }
        val bitsPerIndex = ceil(log2(uniqueNodeData.size.toFloat())).toInt()
        val svoNodes = createSVONodeList(indexedOctree)


        // reduce the indexedOctree to a DAG
        val svoLayers = getSVOLayers(indexedOctree)
        val subtreeMap = mutableMapOf<Int, MutableList<IIndexedOctreeNode>>()
        for (layerIndex in svoLayers.size-1 downTo 0) {
            svoLayers[layerIndex].forEach { subtree ->
                if (subtree is IndexedParentNode) {
                    subtree.children.forEachIndexed{ index, child ->
                        if (child != null) {
                            val hashCode = if (child is IndexedLeafNode) 0 else {
                                child as IndexedParentNode
                                (if (child.children[0] != null) child.children[0]!!.hashValue + 1 shl 0 else 0) +
                                (if (child.children[1] != null) child.children[1]!!.hashValue + 1 shl 1 else 0) +
                                (if (child.children[2] != null) child.children[2]!!.hashValue + 1 shl 2 else 0) +
                                (if (child.children[3] != null) child.children[3]!!.hashValue + 1 shl 3 else 0) +
                                (if (child.children[4] != null) child.children[4]!!.hashValue + 1 shl 4 else 0) +
                                (if (child.children[5] != null) child.children[5]!!.hashValue + 1 shl 5 else 0) +
                                (if (child.children[6] != null) child.children[6]!!.hashValue + 1 shl 6 else 0) +
                                (if (child.children[7] != null) child.children[7]!!.hashValue + 1 shl 7 else 0)
                            }
                            child.hashValue = hashCode
                            val list = subtreeMap[hashCode]
                            if (list == null)
                                subtreeMap[hashCode] = mutableListOf(child)
                            else {
                                var foundMatch = false
                                list.forEachIndexed { i, node ->
                                    if (!foundMatch && (child is IndexedLeafNode && node is IndexedLeafNode) ||
                                        (child is IndexedParentNode && node is IndexedParentNode && child.children.contentDeepEquals(node.children))) {
                                        subtree.children[index] = node
                                        foundMatch = true
                                    }
                                }
                                if (!foundMatch)
                                    list.add(child)
                            }
                        }
                    }
                }
            }
        }
        val dag = indexedOctree

        val textureIndexBufferSize = uniqueNodeData.size * Int.SIZE_BYTES
        val textureIndexBuffer = ByteBuffer.allocate(textureIndexBufferSize)

        val indexBufferSize = ceil((bitsPerIndex * indexedNodes.size) / 8f).toInt()
        val indexBuffer = ByteBuffer.allocate(indexBufferSize)

        val nodeBufferSize = svoNodes.sumOf { Int.SIZE_BYTES + it.childPointers.size * Int.SIZE_BYTES }
        val nodeBuffer = ByteBuffer.allocate(nodeBufferSize)

        return SVOSet(nodeBuffer, bitsPerIndex, indexBuffer, textureIndexBuffer)
    }

    private fun getSVOLayers(indexedOctree: IndexedParentNode): List<List<IIndexedOctreeNode>> {
        val layers : MutableList<MutableList<IIndexedOctreeNode>> = mutableListOf()
        val bfoQueue1 = LinkedTransferQueue<IndexedParentNode>()
        bfoQueue1.put(indexedOctree)
        val bfoQueue2 = LinkedTransferQueue<IndexedParentNode>()
        var list2 = false
        while (bfoQueue1.isNotEmpty() || bfoQueue2.isNotEmpty()) {
            val bfoQueue = if (list2) bfoQueue2 else bfoQueue1
            val otherBfoQueue = if (list2) bfoQueue1 else bfoQueue2
            val currentLayer : MutableList<IIndexedOctreeNode> = mutableListOf()
            while (bfoQueue.isNotEmpty()) {
                val node = bfoQueue.poll()
                currentLayer.add(node)
                node.children.forEach {
                    if (it != null && it is IndexedParentNode) otherBfoQueue.add(it)
                }
            }
            layers.add(currentLayer)
            list2 = !list2
        }

        return layers
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