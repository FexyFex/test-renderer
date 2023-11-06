package me.fexus.examples.hardwarevoxelraytracing.world

import me.fexus.examples.hardwarevoxelraytracing.octree.*
import me.fexus.examples.hardwarevoxelraytracing.util.IntArray3D
import me.fexus.examples.hardwarevoxelraytracing.voxel.VoxelRegistry
import me.fexus.examples.hardwarevoxelraytracing.voxel.type.VoidVoxel
import me.fexus.math.vec.IVec3
import kotlin.math.log2


class Chunk(voxelArray3D: IntArray3D) {
    private var octree: OctreeRootNode

    init {
        this.octree = constructOctreeFrom3DArray(voxelArray3D)
    }


    private fun constructOctreeFrom3DArray(voxelArray3D: IntArray3D): OctreeRootNode {
        val mipLevelCount = log2(EXTENT.toFloat()).toInt()

        val rootNode = OctreeRootNode(IVec3(0), OctreeNodeVoxelData(VoidVoxel), mutableListOf())

        val leafNodes = mutableMapOf<IVec3, IOctreeNode>()

        // Highest mip level leaf node creation
        voxelArray3D.forEachIndexed3D { x, y, z, voxelID ->
            if (voxelID == 0) return@forEachIndexed3D
            val voxel = VoxelRegistry.getVoxelByID(voxelID)
            val position = IVec3(x, y, z)
            val leafNode = OctreeLeafNode(position, OctreeNodeVoxelData(voxel))
            leafNodes[position] = leafNode
        }

        repeat(mipLevelCount) {
            val mipLevel = it + 1
            if (mipLevel == mipLevelCount) {
                if (leafNodes.isEmpty())
            }
        }
    }


    private fun constructOctetRecursive(mipLevel: Int, maxMipLevel: Int, nodes: MutableMap<IVec3, IOctreeNode>): IOctreeNode {
        val currentMipExtent = EXTENT / (2 * mipLevel)
        val newNodes = mutableMapOf<IVec3, IOctreeNode>()
        repeat(8) { index ->
            val parentChunkLocalPosition = localOctPositionFromIndex(index)
            val children = getChildrenOfParent(nodes, parentChunkLocalPosition)
            val parentNode = OctreeNode(parentChunkLocalPosition, children)
            newNodes[parentChunkLocalPosition] = parentNode
        }
        val nextMipLevel = mipLevel + 1
        if (nextMipLevel < maxMipLevel) {
            return OctreeRootNode(IVec3(0), )
        } else {
            val node = OctreeNode()
        }
    }


    private fun getChildrenOfParent(nodes: Map<IVec3, IOctreeNode>, parentPosition: IVec3): MutableList<IOctreeNode> {
        val children = mutableListOf<IOctreeNode>()
        repeat(8) { childIndex ->
            val childPosition = parentPosition + localOctPositionFromIndex(childIndex)
            val childNode = nodes[childPosition] ?: return@repeat
            children.add(childNode)
        }
        return children
    }


    // Index must never exceed 7, but I won't check for that case because that would be kind of sickening
    private fun localOctPositionFromIndex(index: Int): IVec3 {
        val x = index and 1
        val y = (index and 2) shr 1
        val z = index shr 2
        return IVec3(x, y, z)
    }


    companion object {
        const val EXTENT = 2
    }
}