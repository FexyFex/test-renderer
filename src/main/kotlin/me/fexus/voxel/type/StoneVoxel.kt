package me.fexus.voxel.type

import me.fexus.voxel.VoxelColor


data object StoneVoxel: VoxelType() {
    override val name: String = "stone"
    override val color: VoxelColor = VoxelColor.GRAY
}