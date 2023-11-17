package me.fexus.examples.hardwarevoxelraytracing.voxelanimation.model

import me.fexus.examples.hardwarevoxelraytracing.util.IntRange3D
import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.VoxelColorGrid
import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.VoxelHotspot
import me.fexus.math.vec.IVec3
import me.fexus.math.vec.Vec4
import me.fexus.skeletalanimation.Animation
import me.fexus.skeletalanimation.Bone
import me.fexus.skeletalanimation.SkeletalAnimator


abstract class AnimatedVoxelModel(extent: Int) {
    abstract val bones: List<Bone>
    abstract val animations: List<Animation>
    abstract val skeletalAnimator: SkeletalAnimator
    abstract val hotspots: List<VoxelHotspot>
    val voxelGrid = VoxelColorGrid(extent)


    fun updateModel(delta: Float) {
        skeletalAnimator.update(delta)
        voxelGrid.clear()
        hotspots.forEach { hotspot ->
            val targetBone = bones.first { hotspot.parentBoneIndex == it.index }
            val roundedHotSpotPos = (hotspot.positionOffset + targetBone.offset).roundToIVec3()
            val subRangeMin = roundedHotSpotPos - hotspot.range
            val subRangeMax = roundedHotSpotPos + hotspot.range
            val range = IntRange3D(subRangeMin, subRangeMax)
            range.forEach inner@ { x, y, z ->
                val pos = IVec3(x,y,z)
                val color = hotspot.placeVoxel(roundedHotSpotPos - pos)
                if (color.w == 0f) return@inner
                val rotatedPos = (targetBone.animatedTransform * Vec4(x.toFloat(), y.toFloat(), z.toFloat(), 1.0f)).xyz.roundToIVec3()
                if (voxelGrid.isInBounds(rotatedPos))
                    voxelGrid.setVoxelAt(rotatedPos, color)
            }
        }
    }
}