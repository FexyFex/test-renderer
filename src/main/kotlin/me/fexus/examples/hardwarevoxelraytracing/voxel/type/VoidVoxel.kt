package me.fexus.examples.hardwarevoxelraytracing.voxel.type

import me.fexus.examples.hardwarevoxelraytracing.voxel.VoxelColor


data object VoidVoxel: VoxelType() {
    override val name: String = "void"
    override val color: VoxelColor = VoxelColor.INVISIBLE
}