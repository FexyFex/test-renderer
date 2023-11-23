package me.fexus.voxel.octree.buffer

import me.fexus.voxel.octree.IOctreeParentNode
import me.fexus.voxel.octree.OctreeNodeDataVoxelType



data class LastIndexAndNode(val lastIndex: Int, val node: IndexedOctreeNode)
fun createIndexedOctree(parentNode: IOctreeParentNode<OctreeNodeDataVoxelType>, startingIndex: Int): LastIndexAndNode {
    var nextIndex = startingIndex + 1

    val children = mutableListOf<IndexedOctreeChildLink>()
    parentNode.children.forEachIndexed { octantIndex, childNode ->
        if (childNode == null) return@forEachIndexed
        if (childNode is IOctreeParentNode && childNode.hasChildren) {
            val packetThingy = createIndexedOctree(childNode, nextIndex)
            children.add(IndexedOctreeChildLink(octantIndex, packetThingy.node))
            nextIndex = packetThingy.lastIndex
        } else {
            children.add(IndexedOctreeChildLink(octantIndex, IndexedOctreeNode(nextIndex++, childNode.nodeData, emptyList())))
        }
    }

    val indexedNode = IndexedOctreeNode(startingIndex, parentNode.nodeData, children)

    return LastIndexAndNode(nextIndex, indexedNode)
}

fun createIndexedOctreeNodeList(rootNode: IndexedOctreeNode): List<IndexedOctreeNode> {
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