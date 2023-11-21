package me.fexus.voxel

import me.fexus.voxel.type.VoxelType


object VoxelRegistry {
    private val registeredVoxels = Array<VoxelType?>(255) { null }
    private var nextID = 1 // 0 is empty, hence why we start at 1

    fun init() {
        val voxelTypes = VoxelType::class.sealedSubclasses
                .flatMap { it.sealedSubclasses + it }
                .mapNotNull { it.objectInstance }

        voxelTypes.forEach(VoxelRegistry::registerVoxel)
    }


    private fun registerVoxel(voxelType: VoxelType) {
        val id = nextID++
        voxelType.id = id
        registeredVoxels[id] = voxelType
    }


    fun getVoxelByID(id: Int): VoxelType = registeredVoxels[id]!!
}