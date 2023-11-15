package me.fexus.skeletalanimation

import me.fexus.math.mat.Mat4


class SkeletalAnimator(val name: String, val bones: List<Bone>, val animations: List<Animation>) {
    private val rootBone = bones.first { it.parent == null }

    var currentAnimationTime: Float = 0f

    var defaultAnimation: String? = null
    var currentAnimation: Animation? = null
    var nextAnimation: Animation? = null

    init {
        rootBone.calculateInverseBindTransform(Mat4(1f))
    }


    fun update(delta: Float) {
        if (currentAnimation == null) {
            if (defaultAnimation == null) return
            else playAnimation(defaultAnimation!!)
        }
        val currAnim = currentAnimation!!
        currentAnimationTime += delta

        if (currAnim.duration < currentAnimationTime) {
            // animation is OVER
            currentAnimation = null
            if (nextAnimation != null) {
                currentAnimation = nextAnimation
                nextAnimation = null
                currentAnimationTime = 0f
            }
            return
        }

        val currentKeyFrames = currAnim.getPrevAndNextKeyFrame(currentAnimationTime)
        val transforms = interpolatePose(currentAnimationTime, currentKeyFrames)

        applyPoseToBone(transforms, rootBone, rootBone.localBindTransform)
    }


    private fun playAnimation(name: String) {
        val animation = getAnimationByName(name)
        currentAnimation = animation
        currentAnimationTime = 0f
    }

    private fun queueNextAnimation(name: String) {
        if (currentAnimation == null) playAnimation(name) else nextAnimation = getAnimationByName(name)
    }

    private fun applyPoseToBone(transforms: List<BoneTransform>, bone: Bone, parentTransform: Mat4) {
        val currentLocalTransform = transforms.first { it.index == bone.index }.localTransform
        val currentTransform = parentTransform * currentLocalTransform

        bone.children.forEach { childBone ->
            applyPoseToBone(transforms, childBone, currentTransform)
        }
        bone.animatedTransform = currentTransform * bone.inverseBindTransform
    }

    private fun getAnimationByName(name: String) = animations.first { it.name == name }


    private fun interpolatePose(animTimeStamp: Float, keyFrames: HotKeyFrames): List<BoneTransform> {
        val timeDiff = keyFrames.nextKeyFrame.timeStamp - keyFrames.previousKeyFrame.timeStamp
        val interpolationTime = animTimeStamp - keyFrames.previousKeyFrame.timeStamp
        val progress = interpolationTime / timeDiff

        val boneTransforms = Array<BoneTransform>(bones.size) {
            val bone = bones[it]
            BoneTransform(bone.index, bone.offset, bone.animatedTransform.toQuat())
        }

        for (i in 0 until keyFrames.previousKeyFrame.pose.size) {
            val prevBonePose = keyFrames.previousKeyFrame.pose[i]
            val nextBonePose = keyFrames.nextKeyFrame.pose[i]

            val prevRotation = prevBonePose.transform
            val nextRotation = nextBonePose.transform
            val newRotation = prevRotation.slerp(nextRotation, progress).normalize()

            val prevPosition = prevBonePose.position
            val nextPosition = nextBonePose.position
            val diff = nextPosition - prevPosition
            val newPosition = prevPosition + (diff * progress)

            val transform = BoneTransform(prevBonePose.index, newPosition, newRotation)
            boneTransforms[prevBonePose.index] = transform
        }

        return boneTransforms.toList()
    }
}