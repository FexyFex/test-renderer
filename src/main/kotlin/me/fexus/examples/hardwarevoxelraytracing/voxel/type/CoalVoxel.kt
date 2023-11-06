package me.fexus.examples.hardwarevoxelraytracing.voxel.type

import me.fexus.examples.hardwarevoxelraytracing.voxel.VoxelColor


data object CoalVoxel: VoxelType() {
    override val name: String = "coal"
    override val color: VoxelColor = VoxelColor.INVISIBLE
}