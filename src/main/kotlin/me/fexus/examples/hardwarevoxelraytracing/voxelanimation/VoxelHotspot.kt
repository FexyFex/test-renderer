package me.fexus.examples.hardwarevoxelraytracing.voxelanimation

import me.fexus.examples.hardwarevoxelraytracing.voxel.type.VoxelType
import me.fexus.math.vec.IVec3
import me.fexus.math.vec.Vec3


class VoxelHotspot(
    val parentBoneIndex: Int,
    val positionOffset: Vec3,
    val up: Vec3,
    val range: Int,
    val placeVoxel: (relativePosition: IVec3) -> VoxelType
)