package me.fexus.voxel.type

import me.fexus.voxel.VoxelColor


data object DirtVoxel: VoxelType() {
    override val name: String = "dirt"
    override val color: VoxelColor = VoxelColor.GREEN
}