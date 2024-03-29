package me.fexus.voxel

import me.fexus.voxel.type.*


class VoxelRegistry {
    private val registeredVoxels = Array<VoxelType?>(128) { null }
    private var nextID = 0
    var voxelCount: Int = 0; private set


    fun init() {
        // Hardcoding the ids of voxels that are in the GPU worldgen
        registerVoxel(VoidVoxel)
        registerVoxel(StoneVoxel)
        registerVoxel(GrassVoxel)
        registerVoxel(DirtVoxel)

        val voxelTypes = VoxelType::class.sealedSubclasses
            .filter { it != VoidVoxel::class && it != StoneVoxel::class }
            .flatMap { it.sealedSubclasses + it }
            .mapNotNull { it.objectInstance }

        voxelTypes.forEach(this::registerVoxel)
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