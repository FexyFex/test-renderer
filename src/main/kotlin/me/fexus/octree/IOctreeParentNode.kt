package me.fexus.octree

interface IOctreeParentNode<T: IOctreeNodeData>: IOctreeNode<T> {
    val children: Array<IOctreeNode<T>?>

    val childCount: Int; get() = children.count { it != null }
    val hasChildren: Boolean; get() = children.any { it != null }


    fun insert() {

    }
}