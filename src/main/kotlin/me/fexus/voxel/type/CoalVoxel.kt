package me.fexus.voxel.type

import me.fexus.voxel.VoxelColor


data object CoalVoxel: VoxelType() {
    override val name: String = "coal"
    override val color: VoxelColor = VoxelColor.DARK_GRAY
}