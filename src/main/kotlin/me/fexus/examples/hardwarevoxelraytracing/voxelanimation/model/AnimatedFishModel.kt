package me.fexus.examples.hardwarevoxelraytracing.voxelanimation.model

import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.VoxelColorGrid
import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.IntRange3D
import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.VoxelHotspot
import me.fexus.math.mat.Mat4
import me.fexus.math.quat.Quat
import me.fexus.math.random
import me.fexus.math.vec.*
import me.fexus.skeletalanimation.*
import kotlin.math.absoluteValue


class AnimatedFishModel : AnimatedVoxelModel() {
    private var time: Double = 0.0

    private val bones = listOf<Bone>(Bone(0, "root", "root", Vec3(0f), Mat4(), null, mutableListOf()))
    private val animations = listOf<Animation>(
        Animation(
            "exist", listOf(
                KeyFrame(0f, listOf(BoneTransform(0, Vec3(7f), Quat()))),
                KeyFrame(2f, listOf(BoneTransform(0, Vec3(7f), Quat(0f, 1.2f, 0f, 1f)))),
                KeyFrame(6f, listOf(BoneTransform(0, Vec3(7f), Quat(0f, -1.2f, 0f, 1f)))),
                KeyFrame(8f, listOf(BoneTransform(0, Vec3(7f), Quat()))),
            )
        )
    )
    override val skeletalAnimator = SkeletalAnimator("TestBlob", bones, animations)
    private val hotspots = listOf<VoxelHotspot>(
        // Body
        VoxelHotspot(0, Vec3(0f), IVec3(4, 2, 1)) { pos ->
            // round shape
            val naiveDist = pos.y.absoluteValue + pos.x.absoluteValue + pos.z.absoluteValue
            if (naiveDist > 4) return@VoxelHotspot Vec4(0f)
            val blueNoise: Float = random(pos + 6) * 0.5f + 0.4f
            val yellowNoise: Float = random(pos + 2) * 0.17f * (pos.z == 0).toInt() + 0.1f
            //if (random(pos + 11) > 0.995f) return@VoxelHotspot Vec4(1f)
            Vec4(0.1f, yellowNoise, blueNoise, 1f)
        },

        // Eyes
        VoxelHotspot(0, Vec3(-2f, -1f, 1f), IVec3(0)) { Vec4(0f, 0f, 0f, 1f) },
        VoxelHotspot(0, Vec3(-2f, -1f, -1f), IVec3(0)) { Vec4(0f, 0f, 0f, 1f) },

        // Back Fin
        VoxelHotspot(0, Vec3(5f, 0f, 0f), IVec3(1, 3, 0)) { pos ->
            if ((pos.x < 0 && pos.y.absoluteValue == 0) ||
                (pos.y.absoluteValue > 1 && pos.x > 0) ||
                (pos.y.absoluteValue > 2 && pos.x == 0))
                return@VoxelHotspot Vec4(0f)

            Vec4(0.1f, 0.1f, 0.2f + random(pos), 1f)
        },

        // Top Fin
        VoxelHotspot(0, Vec3(1f, -4f, 0f), IVec3(2, 1, 0)) { pos ->
            if (pos.y + pos.x > 0 || (pos.x - pos.y.absoluteValue < -1)) return@VoxelHotspot Vec4(0f)
            Vec4(0.2f, 0.2f, 1f, 1f)
        },

        // Bottom Fin
        VoxelHotspot(0, Vec3(1f, 4f, 0f), IVec3(2, 1, 0)) { pos ->
            if (pos.x - pos.y > 0 || (pos.x - pos.y < -1) || pos.y < 0) return@VoxelHotspot Vec4(0f)
            Vec4(0.2f, 0.2f, 1f, 1f)
        }
    )

    val voxelGrid = VoxelColorGrid(16)


    init {
        skeletalAnimator.defaultAnimation = "exist"
    }


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


    companion object {
        private fun Boolean.toInt() = if (this) 1 else 0
    }
}