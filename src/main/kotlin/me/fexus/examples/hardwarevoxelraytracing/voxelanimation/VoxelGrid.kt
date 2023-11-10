package me.fexus.examples.hardwarevoxelraytracing.voxelanimation

import me.fexus.examples.hardwarevoxelraytracing.octree.*
import me.fexus.examples.hardwarevoxelraytracing.voxel.type.VoidVoxel
import me.fexus.examples.hardwarevoxelraytracing.voxel.type.VoxelType
import me.fexus.math.vec.IVec3
import me.fexus.math.vec.Vec3
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.roundToInt


class VoxelGrid(val extent: Int) {
    val voxelCount: Int = extent * extent * extent
    private val log2 = log2(extent.toFloat())
    private val bounds = 0 until extent
    private val maxMipLevel = log2.roundToInt() - 1

    val octree = OctreeRootNode(IVec3(0), OctreeNodeData(VoidVoxel))

    init {
        if (log2 != ceil(log2)) throw Exception("Only powers of two pls")
    }


    fun setVoxelAt(x: Int, y: Int, z: Int, voxelType: VoxelType) = setVoxelAt(IVec3(x,y,z), voxelType)
    fun setVoxelAt(pos: IVec3, voxelType: VoxelType) {
        assertCoords(pos)
        insertIntoOctreeRec(pos, voxelType, octree, 0)
    }
    private fun insertIntoOctreeRec(pos: IVec3, voxelType: VoxelType, parentNode: IOctreeParentNode, mipLevel: Int) {
        assertCoords(pos)
        val posIndex = getOctantIndexOfMipLevelFromGlobalPosition(pos, mipLevel)

        val targetNode = parentNode.children[posIndex]
        if (targetNode == null) {
            if (voxelType != VoidVoxel)
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
                if (voxelType == VoidVoxel)
                    parentNode.children[posIndex] = null
                else
                    targetNode.nodeData = OctreeNodeData(voxelType)
            }
        }
    }

    fun clear() {
        octree.children.fill(null, 0)
        octree.nodeData = OctreeNodeData(VoidVoxel)
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
        val mipExtent = extent / max(2 * mipLevel, 1)
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


    fun forEachVoxel(action: (x: Int, y: Int, z: Int, voxelType: VoxelType) -> Unit) {
        repeat(extent) { z ->
            repeat(extent) { y ->
                repeat(extent) { x ->
                    action(x,y,z,getVoxelAt(x,y,z))
                }
            }
        }
    }


    companion object {
        private fun Boolean.toInt() = if(this) 1 else 0
    }
}