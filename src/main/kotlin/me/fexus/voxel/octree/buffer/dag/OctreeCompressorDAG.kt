package me.fexus.voxel.octree.buffer.dag

/*
class OctreeCompressorDAG(private val octree: OctreeRootNode<OctreeNodeDataVoxelType>) {
    fun createDAG(): DAGSet {
        val start = System.nanoTime()
        val indexedOctree = createIndexedOctree(octree, 0).node
        val indexedNodes = getNodeList(indexedOctree)
        val uniqueNodeData = indexedNodes.distinctBy { it.nodeData }.map { it.nodeData }
        val bitsPerIndex = ceil(log2(uniqueNodeData.size.toFloat())).toInt()
        val dagNodes = createDAGNodeList(indexedOctree)

        val nodeBuffer = buildSVOBuffer {
            dagNodes.forEach { append(it) }
        }
        val end = System.nanoTime()
        println("Time: ${end - start}")

        val textureIndexBufferSize = uniqueNodeData.size * Int.SIZE_BYTES
        val textureIndexBuffer = ByteBuffer.allocate(textureIndexBufferSize)

        val indexBufferSize = ceil((bitsPerIndex * indexedNodes.size) / 8f).toInt()
        val indexBuffer = ByteBuffer.allocate(indexBufferSize)

        return DAGSet(nodeBuffer, bitsPerIndex, indexBuffer, textureIndexBuffer)
    }

    // A not yet implemented heuristic attempt at merging nodes with the same pointer values.
    // Since checking every node against all others would be costly,
    // we only compare the parent nodes of the lowest layer
    //private fun createTrimmedDAG(dagNodes: List<DAGNode>): List<DAGNode> {
    //}

    private fun getSVOLayers(indexedOctree: IndexedOctreeNode): List<List<IIndexedOctreeNode>> {
        val layers : MutableList<MutableList<IIndexedOctreeNode>> = mutableListOf()
        val bfoQueue1 = LinkedTransferQueue<IndexedOctreeNode>()
        bfoQueue1.put(indexedOctree)
        val bfoQueue2 = LinkedTransferQueue<IndexedOctreeNode>()
        var list2 = false
        while (bfoQueue1.isNotEmpty() || bfoQueue2.isNotEmpty()) {
            val bfoQueue = if (list2) bfoQueue2 else bfoQueue1
            val otherBfoQueue = if (list2) bfoQueue1 else bfoQueue2
            val currentLayer : MutableList<IIndexedOctreeNode> = mutableListOf()
            while (bfoQueue.isNotEmpty()) {
                val node = bfoQueue.poll()
                currentLayer.add(node)
                node.children.forEach {
                    if (it != null && it is IndexedOctreeNode) otherBfoQueue.add(it)
                }
            }
            layers.add(currentLayer)
            list2 = !list2
        }

        return layers
    }

    private fun createDAGNodeList(indexedOctree: IndexedOctreeNode): List<DAGNode> {
        val dagNodes = mutableListOf<DAGNode>()

        fun rec(targetNode: IIndexedOctreeNode) {
            val childPointers = mutableListOf<DAGChildNodePointer>()
            if (targetNode is IndexedOctreeNode) {
                targetNode.children.forEachIndexed { octantIndex, childNode ->
                    if (childNode == null) return@forEachIndexed
                    val pointer = DAGChildNodePointer(octantIndex, childNode.index - targetNode.index, childNode.index)
                    childPointers.add(pointer)
                    rec(childNode)
                }
            }
            val svoNode = DAGNode(targetNode.index, childPointers.size, childPointers)
            dagNodes.add(svoNode)
        }

        rec(indexedOctree)
        return dagNodes.sortedBy { it.index }
    }

    data class LastIndexAndNode(val lastIndex: Int, val node: IndexedOctreeNode)
    private fun createIndexedOctree(parentNode: IOctreeParentNode<OctreeNodeDataVoxelType>, startingIndex: Int): LastIndexAndNode {
        var currentIndex = startingIndex

        val indexedNode = IndexedOctreeNode(currentIndex++, parentNode.nodeData)

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
            if (node is IndexedOctreeNode) allNodes.addAll(node.children.filterNotNull())
            nodeIndex++
        }

        return allNodes
    }
}



/*
        // reduce the indexedOctree to a DAG
        val svoLayers = getSVOLayers(indexedOctree)
        val subtreeMap = mutableMapOf<Int, MutableList<IIndexedOctreeNode>>()
        for (layerIndex in svoLayers.size - 1 downTo 0) {
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
*/