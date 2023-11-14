package me.fexus.examples.hardwarevoxelraytracing.voxelanimation

import me.fexus.math.vec.IVec3
import me.fexus.math.vec.Vec3
import me.fexus.math.vec.Vec4
import me.fexus.octree.*
import kotlin.math.log2
import kotlin.math.roundToInt


class VoxelColorGrid(val extent: Int) {
    val voxelCount = extent * extent * extent
    val bounds = 0 until extent
    val mipLevelCount = log2(extent.toFloat()).roundToInt()
    val maxMipLevel = mipLevelCount - 1

    val octree = OctreeRootNode(IVec3(0), OctreeNodeDataColor(Vec4(0f)))


    fun setVoxelAt(x: Int, y: Int, z: Int, color: Vec4) = setVoxelAt(IVec3(x,y,z), color)
    fun setVoxelAt(pos: IVec3, color: Vec4) {
        assertCoords(pos)
        setVoxelAtRec(pos, color, octree, 0)
    }
    private fun setVoxelAtRec(pos: IVec3, color: Vec4, parentNode: IOctreeParentNode<OctreeNodeDataColor>, mipLevel: Int) {
        assertCoords(pos)
        val posIndex = getOctantIndexOfGlobalPositionInMipLevel(pos, mipLevel)

        val targetNode = parentNode.children[posIndex]
        if (targetNode == null) {
            if (color.w != 0f)
                parentNode.children[posIndex] =
                    if (mipLevel == maxMipLevel) {
                        OctreeLeafNode(pos, OctreeNodeDataColor(color))
                    } else {
                        val newNode = OctreeForkNode(pos, OctreeNodeDataColor(color))
                        setVoxelAtRec(pos, color, newNode, mipLevel + 1)
                        newNode
                    }
        } else {
            if (targetNode is IOctreeParentNode) {
                setVoxelAtRec(pos, color, targetNode, mipLevel + 1)
            } else {
                if (color.w == 0f)
                    parentNode.children[posIndex] = null
                else
                    targetNode.nodeData = OctreeNodeDataColor(color)
            }
        }
    }


    fun getVoxelAt(x: Int, y: Int, z: Int) = getVoxelAt(IVec3(x,y,z))
    fun getVoxelAt(pos: IVec3): Vec4 {
        assertCoords(pos)
        return if (!octree.hasChildren)
            octree.nodeData.color
        else
            getVoxelAtRec(pos, octree, 0)
    }
    private fun getVoxelAtRec(pos: IVec3, parentNode: IOctreeParentNode<OctreeNodeDataColor>, mipLevel: Int): Vec4 {
        val posIndex = getOctantIndexOfGlobalPositionInMipLevel(pos, mipLevel)

        val targetNode = parentNode.children[posIndex]
        if (targetNode == null) {
            return Vec4(0f)
        } else {
            return if (targetNode is IOctreeParentNode) {
                return if (targetNode.hasChildren)
                    getVoxelAtRec(pos, targetNode, mipLevel + 1)
                else
                    targetNode.nodeData.color
            }
            else (targetNode as OctreeLeafNode).nodeData.color
        }
    }

    fun clear() {
        octree.children.fill(null)
        octree.nodeData = OctreeNodeDataColor(Vec4(0f))
    }

    private fun getOctantIndexOfGlobalPositionInMipLevel(globalPosition: IVec3, mipLevel: Int): Int {
        val mipExtent = extent shr mipLevel
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


    fun forEachFilledVoxel(action: (x: Int, y: Int, z: Int, color: Vec4) -> Unit) {
        repeat(extent) { z ->
            repeat(extent) { y ->
                repeat(extent) { x ->
                    val vox = getVoxelAt(x,y,z)
                    if (vox.w > 0f) action (x,y,z, vox)
                }
            }
        }
    }


    companion object {
        private fun Boolean.toInt() = if(this) 1 else 0
    }
}