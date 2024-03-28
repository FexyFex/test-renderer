package me.fexus.examples.coolvoxelrendering.world

import me.fexus.math.repeatCubed
import me.fexus.math.vec.IVec3
import me.fexus.math.vec.Vec3
import me.fexus.voxel.VoxelOctree
import me.fexus.voxel.octree.*


// THIS LOGIC WAS MADE WHOLLY BY ME. IT TOOK AGES TO GET RIGHT. PLEASE CREDIT ME IF YOU COPY IT!!! (no pressure tho)
open class SparseVoxelOctree: VoxelOctree<Int> {
    val octree = OctreeRootNode(IVec3(0), 0)

    override fun setVoxelAt(x: Int, y: Int, z: Int, value: Int) = setVoxelAt(IVec3(x,y,z), value)
    override fun setVoxelAt(pos: IVec3, value: Int) {
        assertCoords(pos)
        insertIntoOctreeRec(pos, value, octree, 0)
    }
    private fun insertIntoOctreeRec(pos: IVec3, value: Int, parentNode: IOctreeParentNode<Int>, currentDepth: Int) {
        if (value != 0 && parentNode.nodeData == value) return

        val posIndex = getOctantIndexByGlobalPositionInDepth(pos, currentDepth)
        val targetNode = parentNode.children[posIndex]

        val nextDepth = currentDepth + 1

        if (targetNode == null) {
            if (value != 0) {
                parentNode.children[posIndex] =
                    if (nextDepth >= VoxelOctree.MAX_DEPTH) {
                        OctreeLeafNode(pos, value)
                    } else {
                        val newNode = OctreeForkNode(pos, 0)
                        insertIntoOctreeRec(pos, value, newNode, nextDepth)
                        newNode
                    }
            }
        } else {
            if (targetNode is IOctreeParentNode) {
                insertIntoOctreeRec(pos, value, targetNode, nextDepth)
            } else {
                if (value == 0)
                    parentNode.children[posIndex] = null
                else
                    targetNode.nodeData = value
            }
        }

        // "Unzip" node if necessary
        if (value == 0 && parentNode.nodeData != 0) {
            val isMaxDepth = nextDepth >= VoxelOctree.MAX_DEPTH
            val childExtent = VoxelOctree.EXTENT ushr nextDepth
            for (currentIndex in 0 until 8) {
                if (currentIndex == posIndex) continue // change all other nodes to the parent data except for the current one
                val position = parentNode.position + (localOctPositionFromIndex(currentIndex) * childExtent)
                parentNode.children[currentIndex] = if (isMaxDepth) OctreeLeafNode(position, parentNode.nodeData)
                                                    else            OctreeForkNode(position, parentNode.nodeData)
            }
            parentNode.nodeData = 0
        }

        // Zip node if possible
        if (value != 0 && parentNode.children.all { it != null && it.nodeData == value }) {
            parentNode.children.fill(null)
            parentNode.nodeData = value
        }
    }

    override fun getVoxelAt(x: Int, y: Int, z: Int, maxMipLevel: Int) = getVoxelAt(IVec3(x,y,z), maxMipLevel)
    override fun getVoxelAt(pos: IVec3, maxDepth: Int): Int {
        assertCoords(pos)
        return getVoxelAtRec(pos, octree, maxDepth, 0)
    }
    private fun getVoxelAtRec(pos: IVec3, parentNode: IOctreeParentNode<Int>, maxDepth: Int, currentDepth: Int): Int {
        val posIndex = getOctantIndexByGlobalPositionInDepth(pos, currentDepth)

        if (!parentNode.hasChildren) return parentNode.nodeData

        val nextDepth = currentDepth + 1
        val targetNode = parentNode.children[posIndex] ?: return 0
        if (nextDepth >= maxDepth) return targetNode.nodeData

        return if (targetNode is IOctreeParentNode) {
            getVoxelAtRec(pos, targetNode, maxDepth, nextDepth)
        } else {
            targetNode.nodeData
        }
    }

    fun clear() {
        octree.children.fill(null)
    }

    private fun getOctantIndexByGlobalPositionInDepth(globalPosition: IVec3, mipLevel: Int): Int {
        val mipExtent = VoxelOctree.EXTENT shr mipLevel
        val rel = (Vec3(globalPosition) / mipExtent).floor()
        val mid = (rel * mipExtent) + (mipExtent / 2f)
        val x = (globalPosition.x >= mid.x).toInt()
        val y = (globalPosition.y >= mid.y).toInt()
        val z = (globalPosition.z >= mid.z).toInt()
        return x or (y shl 1) or (z shl 2)
    }

    override fun forEachVoxel(maxDepth: Int, action: (position: IVec3, value: Int) -> Unit) {
        forEachVoxelRec(octree, IVec3(0), maxDepth, 0, action)
    }
    private fun forEachVoxelRec(
        parent: IOctreeParentNode<Int>,
        parentPos: IVec3,
        maxDepth: Int,
        currentDepth: Int,
        action: (position: IVec3, value: Int) -> Unit
    ) {
        val parentExtent = VoxelOctree.EXTENT ushr currentDepth
        val isFinalDepthLevel = currentDepth >= maxDepth

        if (!parent.hasChildren || isFinalDepthLevel) {
            if (parent.nodeData != 0) {
                val times = parentExtent ushr (VoxelOctree.MAX_DEPTH - maxDepth)
                val mult = VoxelOctree.EXTENT ushr maxDepth
                repeatCubed(times) { x, y, z ->
                    val offset = IVec3(x,y,z) * mult
                    action(parentPos + offset, parent.nodeData)
                }
            }
            return
        }

        val nextDepth = currentDepth + 1
        val childExtent = VoxelOctree.EXTENT ushr nextDepth

        parent.children.forEachIndexed { octantIndex, node ->
            if (node == null) return@forEachIndexed

            val position = localOctPositionFromIndex(octantIndex) * childExtent
            if (node is IOctreeParentNode) {
                forEachVoxelRec(node, parentPos + position, maxDepth, nextDepth, action)
                return@forEachIndexed
            }
            if (node.nodeData != 0) {
                action(parentPos + position, node.nodeData)
            }
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
        private fun Boolean.toInt() = if (this) 1 else 0

        private val IOctreeParentNode<Int>.hasSubVoxels: Boolean
            get() = this.children.any { it != null && it.nodeData != 0 }
    }
}