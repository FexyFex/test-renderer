package me.fexus.voxel

import me.fexus.voxel.type.VoidVoxel
import me.fexus.voxel.type.VoxelType
import me.fexus.math.vec.IVec3
import me.fexus.math.vec.Vec3
import me.fexus.voxel.octree.*


open class SimpleVoxelOctree: VoxelOctree<VoxelType> {
    val octree = OctreeRootNode(IVec3(0), OctreeNodeDataVoxelType(VoidVoxel))

    override fun setVoxelAt(x: Int, y: Int, z: Int, value: VoxelType) = setVoxelAt(IVec3(x,y,z), value)
    override fun setVoxelAt(pos: IVec3, value: VoxelType) {
        assertCoords(pos)
        insertIntoOctreeRec(pos, value, octree, 0)
    }
    private fun insertIntoOctreeRec(pos: IVec3, voxelType: VoxelType, parentNode: IOctreeParentNode<OctreeNodeDataVoxelType>, mipLevel: Int) {
        val posIndex = getOctantIndexByGlobalPositionInMipLevel(pos, mipLevel)

        val targetNode = parentNode.children[posIndex]
        if (targetNode == null) {
            if (voxelType != VoidVoxel)
                parentNode.children[posIndex] =
                    if (mipLevel == (VoxelOctree.MAX_DEPTH - 1)) {
                        OctreeLeafNode(pos, OctreeNodeDataVoxelType(voxelType))
                    } else {
                        val newNode = OctreeForkNode(pos, OctreeNodeDataVoxelType(voxelType))
                        insertIntoOctreeRec(pos, voxelType, newNode, mipLevel + 1)
                        newNode
                    }
        } else {
            if (targetNode is IOctreeParentNode) {
                insertIntoOctreeRec(pos, voxelType, targetNode, mipLevel + 1)
            } else {
                if (voxelType == VoidVoxel)
                    parentNode.children[posIndex] = null
                if (!parentNode.hasChildren) parentNode.nodeData = OctreeNodeDataVoxelType(VoidVoxel)
                else {
                    targetNode.nodeData = OctreeNodeDataVoxelType(voxelType)
                    parentNode.nodeData = OctreeNodeDataVoxelType(voxelType)
                }
            }
        }
    }


    override fun getVoxelAt(x: Int, y: Int, z: Int, maxMipLevel: Int) = getVoxelAt(IVec3(x,y,z), maxMipLevel)
    override fun getVoxelAt(pos: IVec3, maxMipLevel: Int): VoxelType {
        assertCoords(pos)
        return if (!octree.hasChildren)
            octree.nodeData.voxelType
        else
            getVoxelAtRec(pos, octree, maxMipLevel, 0)
    }
    private fun getVoxelAtRec(pos: IVec3, parentNode: IOctreeParentNode<OctreeNodeDataVoxelType>, maxMipLevel: Int, mipLevel: Int): VoxelType {
        val posIndex = getOctantIndexByGlobalPositionInMipLevel(pos, mipLevel)

        val targetNode = parentNode.children[posIndex]
        if (targetNode == null) {
            return VoidVoxel
        } else {
            if (mipLevel >= maxMipLevel) return targetNode.nodeData.voxelType
            return if (targetNode is IOctreeParentNode) {
                return if (targetNode.hasChildren)
                    getVoxelAtRec(pos, targetNode, maxMipLevel, mipLevel + 1)
                else
                    targetNode.nodeData.voxelType
            }
            else (targetNode as OctreeLeafNode).nodeData.voxelType
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

    override fun forEachVoxel(maxDepth: Int, action: (position: IVec3, voxel: VoxelType) -> Unit) {
        forEachVoxelRec(octree, IVec3(0), maxDepth, 1, action)
    }

    private fun forEachVoxelRec(
        parent: IOctreeParentNode<OctreeNodeDataVoxelType>,
        parentPos: IVec3,
        maxDepth: Int,
        mipLevel: Int,
        action: (position: IVec3, voxel: VoxelType) -> Unit
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
            action(parentPos + position, node.nodeData.voxelType)
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