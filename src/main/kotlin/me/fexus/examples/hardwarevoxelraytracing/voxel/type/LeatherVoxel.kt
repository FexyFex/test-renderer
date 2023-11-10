package me.fexus.examples.hardwarevoxelraytracing.voxel.type

import me.fexus.examples.hardwarevoxelraytracing.voxel.VoxelColor


data object LeatherVoxel: VoxelType() {
    override val name: String = "leather"
    override val color: VoxelColor = VoxelColor.BROWN
}