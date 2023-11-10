package me.fexus.examples.hardwarevoxelraytracing.voxel.type

import me.fexus.examples.hardwarevoxelraytracing.voxel.VoxelColor


data object CloudVoxel: VoxelType() {
    override val name: String = "cloud"
    override val color: VoxelColor = VoxelColor.WHITE
}