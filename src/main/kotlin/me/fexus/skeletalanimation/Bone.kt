package me.fexus.skeletalanimation

import me.fexus.math.mat.Mat4
import me.fexus.math.vec.Vec3

data class Bone(
    val index: Int,
    val id: String,
    val name: String,
    val offset: Vec3,
    var localBindTransform: Mat4,
    var parent: Bone?,
    val children: MutableList<Bone>
) {
    var animatedTransform = Mat4()
    var inverseBindTransform = Mat4()

    fun calculateInverseBindTransform(parentBindTransform: Mat4) {
        val bindTransform = parentBindTransform * localBindTransform
        inverseBindTransform = bindTransform.inverse()
        children.forEach { it.calculateInverseBindTransform(bindTransform) }
    }
}