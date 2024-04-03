package me.fexus.camera

import me.fexus.math.mat.Mat4
import me.fexus.math.rad
import me.fexus.math.vec.Vec3
import kotlin.math.abs
import kotlin.math.tan


class CameraOrthographic(var left: Float, var right: Float, var top: Float, var bottom: Float) {
    var position = Vec3(0f)
    var rotation = Vec3(0f)

    var zNear: Float = 0.01f
    var zFar: Float = 1000f


    fun calculateView(): Mat4 {
        val rotation = Mat4(1f)
            .rotate(rotation.x.rad, Vec3(1f, 0f, 0f))
            .rotate(rotation.y.rad, Vec3(0f, 1f, 0f))
            .rotate(rotation.z.rad, Vec3(0f, 0f, 1f))

        val translation = Mat4(1f).translate(position)

        return rotation * translation
    }

    fun calculateProjection(): Mat4 {
        val res = Mat4(1f)

        res[0][0] = 2f / (right - left)
        res[1][1] = 2f / (top - bottom)
        res[3][0] = -(right + left) / (right - left)
        res[3][1] = -(top + bottom) / (top - bottom)

        // ZERO TO ONE CLIP SPACE
        res[2][2] = -1f / (zFar - zNear)
        res[3][2] = -zNear / (zFar - zNear)

        // ONE TO ZERO CLIP SPACE
        //res[2][2] = -2f / (zFar - zNear)
        //res[3][2] = -(zFar + zNear) / (zFar - zNear)

        return res
    }


    companion object {
        private const val epsilonF: Float = 1.1920928955078125e-7f
        private const val epsilon: Double = 2.2204460492503131e-16
    }
}