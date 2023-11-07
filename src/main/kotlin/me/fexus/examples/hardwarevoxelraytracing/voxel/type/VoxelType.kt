package me.fexus.examples.hardwarevoxelraytracing.voxel.type

import me.fexus.examples.hardwarevoxelraytracing.voxel.VoxelColor


sealed class VoxelType {
    abstract val name: String
    abstract val color: VoxelColor


    open var id: Int = -1
}