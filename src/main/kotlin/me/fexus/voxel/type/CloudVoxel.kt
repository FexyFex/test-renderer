package me.fexus.voxel.type

import me.fexus.voxel.VoxelColor


data object CloudVoxel: VoxelType() {
    override val name: String = "cloud"
    override val color: VoxelColor = VoxelColor.WHITE
}