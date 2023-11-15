package me.fexus.math.quat

import me.fexus.math.mat.Mat4
import me.fexus.math.vec.mix
import kotlin.math.sqrt


class Quat(var x: Float, var y: Float, var z: Float, var w: Float) {
    constructor(): this(0f, 0f, 0f, 1f)
    constructor(other: Quat): this(other.x, other.y, other.z, other.w)


    fun toMat4(): Mat4 {
        val res = Mat4()
        val q = this

        val qxx = q.x * q.x
        val qyy = q.y * q.y
        val qzz = q.z * q.z
        val qxz = q.x * q.z
        val qxy = q.x * q.y
        val qyz = q.y * q.z
        val qwx = q.w * q.x
        val qwy = q.w * q.y
        val qwz = q.w * q.z

        res[0][0] = 1f - 2f * (qyy + qzz)
        res[0][1] = 2f * (qxy + qwz)
        res[0][2] = 2f * (qxz - qwy)

        res[1][0] = 2f * (qxy - qwz)
        res[1][1] = 1f - 2f * (qxx + qzz)
        res[1][2] = 2f * (qyz + qwx)

        res[2][0] = 2f * (qxz + qwy)
        res[2][1] = 2f * (qyz - qwx)
        res[2][2] = 1f - 2f * (qxx + qyy)

        return res
    }


    infix fun dot(other: Quat) = this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w

    fun length() = sqrt(this.dot(this))

    fun normalize(): Quat {
        val len = this.length()
        if (len <= 0f) return Quat(0f, 0f, 0f, 1f) // Not sure if this will even happen at all
        val oneOverLen = 1f / len
        return Quat(this.x * oneOverLen, this.y * oneOverLen, this.z * oneOverLen, this.w * oneOverLen)
    }


    fun slerp(b: Quat, interp: Float): Quat {
        val a = this
        val res = Quat()

        var zW = b.w
        var zX = b.x
        var zY = b.y
        var zZ = b.z

        var cosTheta = a dot b

        // If cosTheta < 0, the interpolation will take the long way around the sphere.
        // To fix this, one quat must be negated.
        if (cosTheta < 0f) {
            zW = -b.w
            zX = -b.x
            zY = -b.y
            zZ = -b.z
            cosTheta = -cosTheta
        }

        // Perform a linear interpolation when cosTheta is close to 1 to avoid side effect of sin(angle) becoming a zero denominator
        if (cosTheta > 1f - 1.1920928955078125e-7f) {
            // Linear interpolation
            res.w = mix(a.w, zW, interp)
            res.x = mix(a.x, zX, interp)
            res.y = mix(a.y, zY, interp)
            res.z = mix(a.z, zZ, interp)
            return res
        } else {
            // Essential Mathematics, page 467
            val angle = kotlin.math.acos(cosTheta)
            val s0 = kotlin.math.sin((1f - interp) * angle)
            val s1 = kotlin.math.sin(interp * angle)
            val s2 = kotlin.math.sin(angle)
            res.w = (s0 * a.w + s1 * zW) / s2
            res.x = (s0 * a.x + s1 * zX) / s2
            res.y = (s0 * a.y + s1 * zY) / s2
            res.z = (s0 * a.z + s1 * zZ) / s2
            return res
        }
    }
}