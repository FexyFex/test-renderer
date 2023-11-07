package me.fexus.examples.hardwarevoxelraytracing.octree

interface IOctreeParentNode: IOctreeNode {
    val children: Array<IOctreeNode?>

    val hasChildren: Boolean; get() = children.any { it != null }


    fun insert() {

    }
}