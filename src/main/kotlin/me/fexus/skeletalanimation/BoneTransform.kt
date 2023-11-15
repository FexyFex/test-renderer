package me.fexus.skeletalanimation

import me.fexus.math.mat.Mat4
import me.fexus.math.quat.Quat
import me.fexus.math.vec.Vec3


data class BoneTransform(val index: Int, val position: Vec3, val transform: Quat) {
    val localTransform: Mat4
        get() {
            val mat = Mat4().translate(position)
            return mat * transform.toMat4()
        }
}