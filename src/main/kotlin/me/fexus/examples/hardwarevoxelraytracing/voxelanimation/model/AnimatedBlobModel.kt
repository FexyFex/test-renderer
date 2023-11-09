package me.fexus.examples.hardwarevoxelraytracing.voxelanimation.model

import me.fexus.examples.hardwarevoxelraytracing.voxel.type.CoalVoxel
import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.VoxelGrid
import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.VoxelGridSubRange
import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.VoxelHotspot
import me.fexus.math.mat.Mat4
import me.fexus.math.quat.Quat
import me.fexus.math.vec.*
import me.fexus.skeletalanimation.*
import kotlin.math.round


class AnimatedBlobModel: AnimatedVoxelModel() {
    private var time: Double = 0.0

    private val bones = listOf<Bone>(Bone(0, "root", "root", Vec3(0f), Mat4(), null, mutableListOf()))
    private val animations = listOf<Animation>(
        Animation("exist", listOf(
            KeyFrame(0f, listOf(BoneTransform(0, Vec3(0f), Quat()))),
            KeyFrame(1f, listOf(BoneTransform(1, Vec3(0.25f), Quat()))),
            KeyFrame(1f, listOf(BoneTransform(1, Vec3(0f), Quat())))
        ))
    )
    override val skeletalAnimator = SkeletalAnimator("TestBlob", bones, animations)
    private val hotspots = listOf<VoxelHotspot>(
        VoxelHotspot(0, Vec3(0.5f), Vec3(0, 1, 0), 3) { relPos -> CoalVoxel }
    )

    private val voxelGrid = VoxelGrid(8)

    private val skeletonUpdateInterval = 0.25f // Time until next update in seconds
    private var timeSinceLastSkeletonUpdate: Float = 0f


    init {
        skeletalAnimator.defaultAnimation = "exist"
    }


    fun tick(delta: Float) {
        time += delta
        if (timeSinceLastSkeletonUpdate >= skeletonUpdateInterval) {
            updateModel()
            timeSinceLastSkeletonUpdate = 0f
        } else {
            timeSinceLastSkeletonUpdate += delta
        }
    }


    fun updateModel() {
        skeletalAnimator.update(timeSinceLastSkeletonUpdate)
        voxelGrid.clear()
        hotspots.forEach { hotspot ->
            val targetBone = bones.first { hotspot.parentBoneIndex == it.index }
            val hotspotPos = (targetBone.animatedTransform * Vec4(hotspot.positionOffset + targetBone.offset, 1f)).xyz
            val roundedHotSpotPos = hotspotPos.roundToIVec3()
            val subRangeMin = max(roundedHotSpotPos - hotspot.range, IVec3(0))
            val subRangeMax = min(roundedHotSpotPos + hotspot.range, IVec3(voxelGrid.extent - 1))
            val range = VoxelGridSubRange(subRangeMin, subRangeMax)
            range.forEachVoxel { x, y, z ->
                val pos = IVec3(x,y,z)
                val voxelType = hotspot.placeVoxel(roundedHotSpotPos - pos)
                voxelGrid.setVoxelAt(pos, voxelType)
            }
        }
    }
}