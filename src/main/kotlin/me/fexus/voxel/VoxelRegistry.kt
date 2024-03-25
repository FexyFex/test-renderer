package me.fexus.voxel

import me.fexus.voxel.type.VoidVoxel
import me.fexus.voxel.type.VoxelType


object VoxelRegistry {
    private val registeredVoxels = Array<VoxelType?>(63) { null }
    private var nextID = 1 // 0 is empty, hence why we start at 1
    var voxelCount: Int = 0; private set


    fun init() {
        val voxelTypes = VoxelType::class.sealedSubclasses
            .filter { it != VoidVoxel::class }
            .flatMap { it.sealedSubclasses + it }
            .mapNotNull { it.objectInstance }

        voxelTypes.forEach(VoxelRegistry::registerVoxel)
    }


    private fun registerVoxel(voxelType: VoxelType) {
        val id = nextID++
        voxelType.id = id
        registeredVoxels[id] = voxelType
        voxelCount++
    }


    fun forEachVoxel(action: (voxel: VoxelType) -> Unit) {
        registeredVoxels.forEach {
            if (it != null) action(it)
        }
    }


    fun getVoxelByIDAssert(id: Int): VoxelType = registeredVoxels[id]!!
    fun getVoxelByID(id: Int) = registeredVoxels[id]
}