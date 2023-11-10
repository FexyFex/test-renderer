package me.fexus.examples.hardwarevoxelraytracing.voxelanimation.model

import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.VoxelColorGrid
import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.VoxelGridSubRange
import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.VoxelHotspot
import me.fexus.math.mat.Mat4
import me.fexus.math.quat.Quat
import me.fexus.math.vec.*
import me.fexus.skeletalanimation.*
import java.lang.Math.pow
import kotlin.math.absoluteValue
import kotlin.math.pow


class AnimatedBlobModel: AnimatedVoxelModel() {
    private var time: Double = 0.0

    private val bones = listOf<Bone>(Bone(0, "root", "root", Vec3(8f), Mat4(), null, mutableListOf()))
    private val animations = listOf<Animation>(
        Animation("exist", listOf(
            KeyFrame(0f, listOf(BoneTransform(0, Vec3(-4f, -4f, -3f), Quat()))),
            KeyFrame(5f, listOf(BoneTransform(0, Vec3(-4f, -4f, -3f), Quat()))),
            //KeyFrame(3f, listOf(BoneTransform(0, Vec3(-15f), Quat())))
        ))
    )
    override val skeletalAnimator = SkeletalAnimator("TestBlob", bones, animations)
    private val hotspots = listOf<VoxelHotspot>(
        VoxelHotspot(0, Vec3(0f, 0f, 3f), Vec3(0, 1, 0), 1) { relPos ->
            val len = relPos.x.absoluteValue + relPos.y.absoluteValue + relPos.z.absoluteValue
            if (len > 1) Vec4(0f) else Vec4(0f,0f,0f,1f)
        },
        VoxelHotspot(0, Vec3(0f, 0f, -3f), Vec3(0, 1, 0), 1) { relPos ->
            val len = relPos.x.absoluteValue + relPos.y.absoluteValue + relPos.z.absoluteValue
            if (len > 1) Vec4(0f) else Vec4(0f,0f,0f,1f)
        },
    )

    val voxelGrid = VoxelColorGrid(32)

    private val skeletonUpdateInterval = 0.125f // Time until next update in seconds
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
            val subRangeMax = min(subRangeMin + hotspot.range * 2, IVec3(voxelGrid.extent - 1))
            val range = VoxelGridSubRange(subRangeMin, subRangeMax)
            range.forEachVoxel { x, y, z ->
                val pos = IVec3(x,y,z)
                val voxelType = hotspot.placeVoxel(roundedHotSpotPos - pos)
                voxelGrid.setVoxelAt(pos, voxelType)
            }
        }
    }
}