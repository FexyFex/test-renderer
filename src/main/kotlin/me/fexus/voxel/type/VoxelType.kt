package me.fexus.voxel.type

import me.fexus.voxel.VoxelColor


sealed class VoxelType {
    abstract val name: String
    abstract val color: VoxelColor


    open var id: Int = -1
}