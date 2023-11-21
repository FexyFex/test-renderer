package me.fexus.voxel.type

import me.fexus.voxel.VoxelColor


data object VoidVoxel: VoxelType() {
    override var id = 0

    override val name: String = "void"
    override val color: VoxelColor = VoxelColor.INVISIBLE
}