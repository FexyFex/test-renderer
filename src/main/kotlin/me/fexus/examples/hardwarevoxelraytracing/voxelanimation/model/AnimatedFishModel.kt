package me.fexus.examples.hardwarevoxelraytracing.voxelanimation.model

import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.VoxelColorGrid
import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.VoxelGridSubRange
import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.VoxelHotspot
import me.fexus.math.mat.Mat4
import me.fexus.math.quat.Quat
import me.fexus.math.vec.*
import me.fexus.skeletalanimation.*
import kotlin.math.absoluteValue
import kotlin.math.sign


class AnimatedFishModel : AnimatedVoxelModel() {
    private var time: Double = 0.0

    private val bones = listOf<Bone>(Bone(0, "root", "root", Vec3(0f), Mat4(), null, mutableListOf()))
    private val animations = listOf<Animation>(
        Animation(
            "exist", listOf(
                KeyFrame(0f, listOf(BoneTransform(0, Vec3(8f), Quat()))),
                KeyFrame(3f, listOf(BoneTransform(0, Vec3(16f), Quat(0f, 2f, 0f, 0.9f)))),
                KeyFrame(6f, listOf(BoneTransform(0, Vec3(8f), Quat()))),
            )
        )
    )
    override val skeletalAnimator = SkeletalAnimator("TestBlob", bones, animations)
    private val hotspots = listOf<VoxelHotspot>(
        // Body
        VoxelHotspot(0, Vec3(0f), 4) { relPos ->
            val len = relPos.length
            if (len > 2.3f) Vec4(0f) else {
                if (relPos.x < 0 && len < 0.5f) return@VoxelHotspot Vec4(0f)
                val e = relPos.y.sign * len / 11f
                Vec4(0.7f + e, 0.2f, 0.2f, 1.0f)
            }
        },

        // Eyes
        VoxelHotspot(0, Vec3(-2f, -1f, 1f), 0) { Vec4(0f, 0f, 0f, 1f) },
        VoxelHotspot(0, Vec3(-2f, -1f, -1f), 0) { Vec4(0f, 0f, 0f, 1f) },

        // Fin
        VoxelHotspot(0, Vec3(3f, 0f, 0f), 1) { relPos ->
            val onZ = (relPos.z == 0) && relPos.x <= 0 && (relPos.y.absoluteValue < 1 || relPos.x < 0)
            Vec4(0.5f, 0.1f, 0.2f, onZ.toInt())
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
                val pos = IVec3(x, y, z)
                val voxelColor = hotspot.placeVoxel(roundedHotSpotPos - pos)
                if (voxelColor.w == 0f) return@forEachVoxel
                voxelGrid.setVoxelAt(pos, voxelColor)
            }
        }
    }



    companion object {
        private fun Boolean.toInt() = if (this) 1 else 0
    }
}