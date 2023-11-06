package me.fexus.examples.hardwarevoxelraytracing.voxel.type

import me.fexus.examples.hardwarevoxelraytracing.voxel.VoxelColor


data object StoneVoxel: VoxelType() {
    override val name: String = "stone"
    override val color: VoxelColor = VoxelColor.GRAY
}