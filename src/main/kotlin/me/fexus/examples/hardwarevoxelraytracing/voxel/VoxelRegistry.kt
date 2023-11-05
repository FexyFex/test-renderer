package me.fexus.examples.hardwarevoxelraytracing.voxel

import me.fexus.examples.hardwarevoxelraytracing.voxel.type.VoxelType


class VoxelRegistry {
    private val registeredVoxels = mutableListOf<VoxelType>()
    private var nextID = 1 // 0 is empty, hence why we start at 1

    init {
        val voxelTypes = VoxelType::class.sealedSubclasses
                .flatMap { it.sealedSubclasses + it }
                .mapNotNull { it.objectInstance }

        voxelTypes.forEach(::registerVoxel)
    }


    private fun registerVoxel(voxelType: VoxelType) {
        val id = nextID++
        voxelType.id = id
        registeredVoxels.add(id, voxelType)
    }


    fun getVoxelByID(id: Int): VoxelType = registeredVoxels[id]
}