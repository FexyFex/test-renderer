package me.fexus.examples.hardwarevoxelraytracing.voxelanimation.model

import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.VoxelColorGrid
import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.VoxelHotspot
import me.fexus.skeletalanimation.SkeletalAnimator


abstract class AnimatedVoxelModel(extent: Int) {
    abstract val skeletalAnimator: SkeletalAnimator
    abstract val hotspots: List<VoxelHotspot>
    val voxelGrid = VoxelColorGrid(extent)
}