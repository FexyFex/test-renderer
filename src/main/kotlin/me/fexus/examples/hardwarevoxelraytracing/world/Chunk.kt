package me.fexus.examples.hardwarevoxelraytracing.world

import me.fexus.examples.hardwarevoxelraytracing.octree.*
import me.fexus.examples.hardwarevoxelraytracing.voxel.type.VoidVoxel
import me.fexus.examples.hardwarevoxelraytracing.voxel.type.VoxelType
import me.fexus.math.vec.IVec3
import me.fexus.math.vec.Vec3
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.roundToInt


class Chunk {
    val octree: IOctreeParentNode = OctreeRootNode(IVec3(0), OctreeNodeData(VoidVoxel))


    fun insertIntoOctree(x: Int, y: Int, z: Int, voxelType: VoxelType) = insertIntoOctree(IVec3(x,y,z), voxelType)
    fun insertIntoOctree(pos: IVec3, voxelType: VoxelType) {
        assertCoords(pos)
        insertIntoOctreeRec(pos, voxelType, octree, 0)
    }
    private fun insertIntoOctreeRec(pos: IVec3, voxelType: VoxelType, parentNode: IOctreeParentNode, mipLevel: Int) {
        val posIndex = getOctantIndexOfMipLevelFromGlobalPosition(pos, mipLevel)

        val targetNode = parentNode.children[posIndex]
        if (targetNode == null) {
            parentNode.children[posIndex] =
                if (mipLevel == maxMipLevel) {
                    OctreeLeafNode(pos, OctreeNodeData(voxelType))
                } else {
                    val newNode = OctreeNode(pos, OctreeNodeData(voxelType))
                    insertIntoOctreeRec(pos, voxelType, newNode, mipLevel + 1)
                    newNode
                }
        } else {
            if (targetNode is IOctreeParentNode) {
                insertIntoOctreeRec(pos, voxelType, targetNode, mipLevel + 1)
            } else {
                targetNode.nodeData = OctreeNodeData(voxelType)
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
    private fun getVoxelAtRec(pos: IVec3, parentNode: IOctreeParentNode, mipLevel: Int): VoxelType {
        val posIndex = getOctantIndexOfMipLevelFromGlobalPosition(pos, mipLevel)

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

    private fun getOctantIndexOfMipLevelFromGlobalPosition(globalPosition: IVec3, mipLevel: Int): Int {
        val mipExtent = EXTENT / max(2 * mipLevel, 1)
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
        const val EXTENT = 2
        private val bounds = 0 until EXTENT
        private val min = IVec3(0)
        private val max = IVec3(EXTENT - 1)
        private val maxMipLevel = log2(EXTENT.toFloat()).roundToInt()

        private fun Boolean.toInt() = if(this) 1 else 0
    }
}