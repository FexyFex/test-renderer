package me.fexus.voxel.octree

interface IOctreeParentNode<T>: IOctreeNode<T> {
    val children: Array<IOctreeNode<T>?>

    val childCount: Int; get() = children.count { it != null }
    val hasChildren: Boolean; get() = children.any { it != null }
}