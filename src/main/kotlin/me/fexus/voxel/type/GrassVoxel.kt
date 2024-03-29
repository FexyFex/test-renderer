package me.fexus.voxel.type

import me.fexus.voxel.VoxelColor


data object GrassVoxel: VoxelType() {
    override val name: String = "grass"
    override val color: VoxelColor = VoxelColor.DARK_BROWN
}