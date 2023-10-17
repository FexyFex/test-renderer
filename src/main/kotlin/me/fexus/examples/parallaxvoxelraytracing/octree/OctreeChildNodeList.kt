package me.fexus.examples.parallaxvoxelraytracing.octree

class OctreeChildNodeList {
    private val arr = Array<OctreeNode>(8) { EmptyLeafNode(emptyList()) }

    operator fun get(index: Int): OctreeNode {
        return arr[index]
    }
}