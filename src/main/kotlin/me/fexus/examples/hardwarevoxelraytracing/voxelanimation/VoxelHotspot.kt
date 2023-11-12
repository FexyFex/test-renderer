package me.fexus.examples.hardwarevoxelraytracing.voxelanimation

import me.fexus.math.vec.IVec3
import me.fexus.math.vec.Vec3
import me.fexus.math.vec.Vec4


class VoxelHotspot(
    val parentBoneIndex: Int,
    val positionOffset: Vec3,
    val range: Int,
    val placeVoxel: (relativePosition: IVec3) -> Vec4
)