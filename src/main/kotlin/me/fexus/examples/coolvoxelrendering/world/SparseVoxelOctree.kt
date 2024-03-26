package me.fexus.examples.coolvoxelrendering.world

import me.fexus.math.vec.IVec3
import me.fexus.math.vec.Vec3
import me.fexus.voxel.VoxelOctree
import me.fexus.voxel.octree.*


open class SparseVoxelOctree: VoxelOctree<Int> {
    val octree = OctreeRootNode(IVec3(0), 0)

    override fun setVoxelAt(x: Int, y: Int, z: Int, value: Int) = setVoxelAt(IVec3(x,y,z), value)
    override fun setVoxelAt(pos: IVec3, value: Int) {
        assertCoords(pos)
        insertIntoOctreeRec(pos, value, octree, 0)
    }
    private fun insertIntoOctreeRec(pos: IVec3, value: Int, parentNode: IOctreeParentNode<Int>, mipLevel: Int) {
        if (value != 0 && parentNode.nodeData == value) return
        val posIndex = getOctantIndexByGlobalPositionInMipLevel(pos, mipLevel)

        val targetNode = parentNode.children[posIndex]
        if (value != 0 && targetNode?.nodeData == value) return
        if (targetNode == null) {
            if (value != 0)
                parentNode.children[posIndex] =
                    if (mipLevel == VoxelOctree.MAX_DEPTH) {
                        OctreeLeafNode(pos, value)
                    } else {
                        val newNode = OctreeForkNode(pos, 0)
                        insertIntoOctreeRec(pos, value, newNode, mipLevel + 1)
                        newNode
                    }
        } else {
            if (targetNode is IOctreeParentNode) {
                insertIntoOctreeRec(pos, value, targetNode, mipLevel + 1)
            } else {
                if (value == 0)
                    parentNode.children[posIndex] = null
                else
                    targetNode.nodeData = value
            }
        }

        // Clear children if they are all the same
        if (parentNode.children.all { it?.nodeData == value }) {
            parentNode.children.fill(null)
            parentNode.nodeData = value
        }
        if (parentNode.children.all { it == null || it.nodeData == 0}) {
            parentNode.nodeData = 0
        }
    }

    override fun getVoxelAt(x: Int, y: Int, z: Int, maxMipLevel: Int) = getVoxelAt(IVec3(x,y,z), maxMipLevel)
    override fun getVoxelAt(pos: IVec3, maxMipLevel: Int): Int {
        assertCoords(pos)
        return getVoxelAtRec(pos, octree, maxMipLevel, 0)
    }
    private fun getVoxelAtRec(pos: IVec3, parentNode: IOctreeParentNode<Int>, maxMipLevel: Int, mipLevel: Int): Int {
        val posIndex = getOctantIndexByGlobalPositionInMipLevel(pos, mipLevel)

        if (!parentNode.hasChildren) return parentNode.nodeData
        val targetNode = parentNode.children[posIndex]
        if (targetNode == null) {
            return 0
        } else {
            if (mipLevel >= maxMipLevel) return targetNode.nodeData
            return if (targetNode is IOctreeParentNode) {
                return if (targetNode.hasChildren)
                    getVoxelAtRec(pos, targetNode, maxMipLevel, mipLevel + 1)
                else
                    targetNode.nodeData
            } else {
                (targetNode as OctreeLeafNode).nodeData
            }
        }
    }

    fun clear() {
        octree.children.fill(null)
    }

    private fun getOctantIndexByGlobalPositionInMipLevel(globalPosition: IVec3, mipLevel: Int): Int {
        val mipExtent = VoxelOctree.EXTENT shr mipLevel
        val rel = (Vec3(globalPosition) / mipExtent).floor()
        val mid = (rel * mipExtent) + (mipExtent / 2f)
        val x = (globalPosition.x >= mid.x).toInt()
        val y = (globalPosition.y >= mid.y).toInt()
        val z = (globalPosition.z >= mid.z).toInt()
        return x or (y shl 1) or (z shl 2)
    }

    override fun forEachVoxel(maxDepth: Int, action: (position: IVec3, value: Int) -> Unit) {
        forEachVoxelRec(octree, IVec3(0), maxDepth, 1, action)
    }

    private fun forEachVoxelRec(
        parent: IOctreeParentNode<Int>,
        parentPos: IVec3,
        maxDepth: Int,
        mipLevel: Int,
        action: (position: IVec3, value: Int) -> Unit
    ) {
        val extent = VoxelOctree.EXTENT ushr mipLevel
        val isFinalMipLevel = mipLevel > maxDepth
        parent.children.forEachIndexed { octantIndex, node ->
            if (node == null) return@forEachIndexed

            val position = localOctPositionFromIndex(octantIndex) * extent
            if (node is IOctreeParentNode && node.hasChildren && !isFinalMipLevel) {
                forEachVoxelRec(node, parentPos + position, maxDepth, mipLevel + 1, action)
                return@forEachIndexed
            }
            action(parentPos + position, node.nodeData)
        }
    }


    // Index must never exceed 7, but I won't check for that case because that would be a lot of assertions
    private fun localOctPositionFromIndex(index: Int): IVec3 {
        val x = index and 1
        val y = (index and 2) shr 1
        val z = index shr 2
        return IVec3(x, y, z)
    }


    private fun assertCoords(pos: IVec3) = assertCoords(pos.x, pos.y, pos.z)
    private fun assertCoords(x: Int, y: Int, z: Int) {
        if (x !in VoxelOctree.BOUNDS || y !in VoxelOctree.BOUNDS || z !in VoxelOctree.BOUNDS)
            throw Exception("Out of bounds $x, $y, $z")
    }


    companion object {
        private fun Boolean.toInt() = if(this) 1 else 0
    }
}