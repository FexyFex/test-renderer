package me.fexus.voxel

import me.fexus.voxel.type.VoidVoxel
import me.fexus.voxel.type.VoxelType
import me.fexus.math.vec.IVec3
import me.fexus.math.vec.Vec3
import me.fexus.voxel.octree.*
import kotlin.math.*


class SparseVoxelOctree {
    val octree = OctreeRootNode(IVec3(0), OctreeNodeDataVoxelType(VoidVoxel))

    fun insertIntoOctree(x: Int, y: Int, z: Int, voxelType: VoxelType) = insertIntoOctree(IVec3(x,y,z), voxelType)
    fun insertIntoOctree(pos: IVec3, voxelType: VoxelType) {
        assertCoords(pos)
        insertIntoOctreeRec(pos, voxelType, octree, 0)
    }
    private fun insertIntoOctreeRec(pos: IVec3, voxelType: VoxelType, parentNode: IOctreeParentNode<OctreeNodeDataVoxelType>, mipLevel: Int) {
        assertCoords(pos)
        val posIndex = getOctantIndexOfGlobalPositionInMipLevel(pos, mipLevel)

        val targetNode = parentNode.children[posIndex]
        if (targetNode == null) {
            if (voxelType != VoidVoxel)
                parentNode.children[posIndex] =
                    if (mipLevel == maxMipLevel) {
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
                else
                    targetNode.nodeData = OctreeNodeDataVoxelType(voxelType)
            }
        }
    }


    fun getVoxelAt(x: Int, y: Int, z: Int) = getVoxelAt(IVec3(x,y,z))
    fun getVoxelAt(pos: IVec3): VoxelType {
        assertCoords(pos)
        return if (!octree.hasChildren)
            octree.nodeData.voxelType
        else
            getVoxelAtRec(pos, octree, 0)
    }
    private fun getVoxelAtRec(pos: IVec3, parentNode: IOctreeParentNode<OctreeNodeDataVoxelType>, mipLevel: Int): VoxelType {
        val posIndex = getOctantIndexOfGlobalPositionInMipLevel(pos, mipLevel)

        val targetNode = parentNode.children[posIndex]
        if (targetNode == null) {
            return VoidVoxel
        } else {
            return if (targetNode is IOctreeParentNode) {
                return if (targetNode.hasChildren)
                    getVoxelAtRec(pos, targetNode, mipLevel + 1)
                else
                    targetNode.nodeData.voxelType
            }
            else (targetNode as OctreeLeafNode).nodeData.voxelType
        }
    }

    private fun getOctantIndexOfGlobalPositionInMipLevel(globalPosition: IVec3, mipLevel: Int): Int {
        val mipExtent = EXTENT shr mipLevel
        val rel = (Vec3(globalPosition) / mipExtent).floor()
        val mid = (rel * mipExtent) + (mipExtent / 2f)
        val x = (globalPosition.x >= mid.x).toInt()
        val y = (globalPosition.y >= mid.y).toInt()
        val z = (globalPosition.z >= mid.z).toInt()
        return x or (y shl 1) or (z shl 2)
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
        if (x !in bounds || y !in bounds || z !in bounds)
            throw AssertionError("Out of bounds $x, $y, $z")
    }


    companion object {
        const val EXTENT = 16
        private val bounds = 0 until EXTENT
        private val maxMipLevel = log2(EXTENT.toFloat()).roundToInt() - 1

        private fun Boolean.toInt() = if(this) 1 else 0
    }
}