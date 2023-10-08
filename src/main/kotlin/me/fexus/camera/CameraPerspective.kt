package me.fexus.camera

import me.fexus.math.mat.Mat4
import me.fexus.math.rad
import me.fexus.math.vec.Vec3
import kotlin.math.abs
import kotlin.math.tan


class CameraPerspective(var aspect: Float) {
    var position = Vec3(0f)
    var rotation = Vec3(0f)

    var fov: Float = 60f
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

        if (abs(aspect - epsilonF) > 0f) throw Exception("WeeeeWooo")

        val tanHalfFov = tan(fov.rad / 2f)

        val res = Mat4(0f)
        res[0][0] = 1f / (aspect * tanHalfFov)
        res[1][1] = 1f / tanHalfFov
        res[2][3] = -1f
        res[2][2] = zFar / (zNear - zFar)
        res[3][2] = -(zFar * zNear) / (zFar - zNear)

        return res
    }

    fun calculateReverseZProjection(): Mat4 {
        val thing = 1.0f / tan(fov.rad / 2.0f)
        val proj = Mat4(
            thing / aspect, 0.0f,  0.0f,  0.0f,
            0.0f,    thing,  0.0f,  0.0f,
            0.0f, 0.0f,  0.0f, -1.0f,
            0.0f, 0.0f, zNear,  0.0f
        )
        proj[1][1] *= -1f
        return proj
    }


    companion object {
        private const val epsilonF: Float = 1.1920928955078125e-7f
        private const val epsilon: Double = 2.2204460492503131e-16
    }
}