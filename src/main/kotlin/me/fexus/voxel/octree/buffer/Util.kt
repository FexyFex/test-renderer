package me.fexus.voxel.octree.buffer

import me.fexus.voxel.octree.IOctreeParentNode
import me.fexus.voxel.octree.OctreeNodeDataVoxelType


data class LastIndexAndNode(val lastIndex: Int, val node: IndexedOctreeNode)
fun createIndexedOctree(parentNode: IOctreeParentNode<OctreeNodeDataVoxelType>, startingIndex: Int): LastIndexAndNode {
    var currentIndex = startingIndex

    val children = mutableListOf<IndexedOctreeChildLink>()
    parentNode.children.forEachIndexed { index, childNode ->
        if (childNode == null) return@forEachIndexed
        if (childNode is IOctreeParentNode) {
            val packetThingy = createIndexedOctree(childNode, currentIndex++)
            currentIndex = packetThingy.lastIndex
            children.add(IndexedOctreeChildLink(index, IndexedOctreeNode(currentIndex++, childNode.nodeData, packetThingy.node.children)))
        } else {
            children.add(IndexedOctreeChildLink(index, IndexedOctreeNode(currentIndex++, childNode.nodeData, emptyList())))
        }
    }

    val indexedNode = IndexedOctreeNode(currentIndex++, parentNode.nodeData, children)

    return LastIndexAndNode(currentIndex, indexedNode)
}

fun getNodeList(rootNode: IndexedOctreeNode): List<IndexedOctreeNode> {
    val allNodes = mutableListOf<IndexedOctreeNode>()
    allNodes.add(rootNode)
    var nodeIndex = 0

    while (nodeIndex < allNodes.size) {
        val node = allNodes[nodeIndex]
        allNodes.addAll(node.children.map { it.child })
        nodeIndex++
    }

    return allNodes
}