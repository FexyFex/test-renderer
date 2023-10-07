package me.fexus.math.mat

import me.fexus.math.inverseSqrt
import me.fexus.math.repeatSqaued
import me.fexus.math.vec.Vec3
import me.fexus.math.vec.Vec4
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sin


class Mat4(val columns: Array<Vec4>) {
    constructor(): this(1f)
    constructor(s: Float): this(s,s,s,s)
    constructor(x: Float, y: Float, z: Float, w: Float): this(
        arrayOf(
            Vec4(x, 0f, 0f, 0f),
            Vec4(0f, y, 0f, 0f),
            Vec4(0f, 0f, z, 0f),
            Vec4(0f, 0f, 0f, z)
        )
    )
    constructor(
        xx: Float, xy: Float, xz: Float, xw: Float,
        yx: Float, yy: Float, yz: Float, yw: Float,
        zx: Float, zy: Float, zz: Float, zw: Float,
        wx: Float, wy: Float, wz: Float, ww: Float
    ): this(
        arrayOf(
            Vec4(xx, xy, xz, xw),
            Vec4(yx, yy, yz, yw),
            Vec4(zx, zy, zz, zw),
            Vec4(wx, wy, wz, ww)
        )
    )

    val x: Vec4; get() = columns[0]
    val y: Vec4; get() = columns[1]
    val z: Vec4; get() = columns[2]
    val w: Vec4; get() = columns[3]

    operator fun get(index: Int) = columns[index]

    fun put(other: Mat4) {
        repeatSqaued(4) { x, y ->
            this.columns[x][y] = other.columns[x][y]
        }
    }

    fun translate(translate: Vec3): Mat4 {
        val m = Mat4()
        m.put(this)
        m[3].x = m[0].x * translate.x + m[1].x * translate.y + m[2].x * translate.z + m[3].x
        m[3].y = m[0].y * translate.x + m[1].y * translate.y + m[2].y * translate.z + m[3].y
        m[3].z = m[0].z * translate.x + m[1].z * translate.y + m[2].z * translate.z + m[3].z
        m[3].w = m[0].w * translate.x + m[1].w * translate.y + m[2].w * translate.z + m[3].w
        return m
    }

    fun rotate(angle: Float, rotation: Vec3): Mat4 {
        val res = Mat4()

        val c = cos(angle)
        val s = sin(angle)

        val dot = rotation.x * rotation.x + rotation.y * rotation.y + rotation.z * rotation.z
        val inv = inverseSqrt(dot)

        val axisX = rotation.x * inv
        val axisY = rotation.y * inv
        val axisZ = rotation.z * inv

        val tempX = (1f - c) * axisX
        val tempY = (1f - c) * axisY
        val tempZ = (1f - c) * axisZ

        val rotate00 = c + tempX * axisX
        val rotate01 = tempX * axisY + s * axisZ
        val rotate02 = tempX * axisZ - s * axisY

        val rotate10 = tempY * axisX - s * axisZ
        val rotate11 = c + tempY * axisY
        val rotate12 = tempY * axisZ + s * axisX

        val rotate20 = tempZ * axisX + s * axisY
        val rotate21 = tempZ * axisY - s * axisX
        val rotate22 = c + tempZ * axisZ

        val res0x = this[0].x * rotate00 + this[1].x * rotate01 + this[2].x * rotate02
        val res0y = this[0].y * rotate00 + this[1].y * rotate01 + this[2].y * rotate02
        val res0z = this[0].z * rotate00 + this[1].z * rotate01 + this[2].z * rotate02
        val res0w = this[0].w * rotate00 + this[1].w * rotate01 + this[2].w * rotate02

        val res1x = this[0].x * rotate10 + this[1].x * rotate11 + this[2].x * rotate12
        val res1y = this[0].y * rotate10 + this[1].y * rotate11 + this[2].y * rotate12
        val res1z = this[0].z * rotate10 + this[1].z * rotate11 + this[2].z * rotate12
        val res1w = this[0].w * rotate10 + this[1].w * rotate11 + this[2].w * rotate12

        val res2x = this[0].x * rotate20 + this[1].x * rotate21 + this[2].x * rotate22
        val res2y = this[0].y * rotate20 + this[1].y * rotate21 + this[2].y * rotate22
        val res2z = this[0].z * rotate20 + this[1].z * rotate21 + this[2].z * rotate22
        val res2w = this[0].w * rotate20 + this[1].w * rotate21 + this[2].w * rotate22

        res[0].x = res0x
        res[0].y = res0y
        res[0].z = res0z
        res[0].w = res0w

        res[1].x = res1x
        res[1].y = res1y
        res[1].z = res1z
        res[1].w = res1w

        res[2].x = res2x
        res[2].y = res2y
        res[2].z = res2z
        res[2].w = res2w

        res[3].x = this[3].x
        res[3].y = this[3].y
        res[3].z = this[3].z
        res[3].w = this[3].w

        return res
    }

    fun scale(scale: Vec3): Mat4 {
        val m = Mat4()
        m.put(this)

        m[0].x = m[0].x * scale.x
        m[0].y = m[0].y * scale.x
        m[0].z = m[0].z * scale.x
        m[0].w = m[0].w * scale.x

        m[1].x = m[1].x * scale.y
        m[1].y = m[1].y * scale.y
        m[1].z = m[1].z * scale.y
        m[1].w = m[1].w * scale.y

        m[2].x = m[2].x * scale.z
        m[2].y = m[2].y * scale.z
        m[2].z = m[2].z * scale.z
        m[2].w = m[2].w * scale.z

        return m
    }


    operator fun times(b: Mat4): Mat4 {
        val res = Mat4(1f)
        val a = this

        val v00 = a[0][0] * b[0][0] + a[1][0] * b[0][1] + a[2][0] * b[0][2] + a[3][0] * b[0][3]
        val v01 = a[0][1] * b[0][0] + a[1][1] * b[0][1] + a[2][1] * b[0][2] + a[3][1] * b[0][3]
        val v02 = a[0][2] * b[0][0] + a[1][2] * b[0][1] + a[2][2] * b[0][2] + a[3][2] * b[0][3]
        val v03 = a[0][3] * b[0][0] + a[1][3] * b[0][1] + a[2][3] * b[0][2] + a[3][3] * b[0][3]
        val v10 = a[0][0] * b[1][0] + a[1][0] * b[1][1] + a[2][0] * b[1][2] + a[3][0] * b[1][3]
        val v11 = a[0][1] * b[1][0] + a[1][1] * b[1][1] + a[2][1] * b[1][2] + a[3][1] * b[1][3]
        val v12 = a[0][2] * b[1][0] + a[1][2] * b[1][1] + a[2][2] * b[1][2] + a[3][2] * b[1][3]
        val v13 = a[0][3] * b[1][0] + a[1][3] * b[1][1] + a[2][3] * b[1][2] + a[3][3] * b[1][3]
        val v20 = a[0][0] * b[2][0] + a[1][0] * b[2][1] + a[2][0] * b[2][2] + a[3][0] * b[2][3]
        val v21 = a[0][1] * b[2][0] + a[1][1] * b[2][1] + a[2][1] * b[2][2] + a[3][1] * b[2][3]
        val v22 = a[0][2] * b[2][0] + a[1][2] * b[2][1] + a[2][2] * b[2][2] + a[3][2] * b[2][3]
        val v23 = a[0][3] * b[2][0] + a[1][3] * b[2][1] + a[2][3] * b[2][2] + a[3][3] * b[2][3]
        val v30 = a[0][0] * b[3][0] + a[1][0] * b[3][1] + a[2][0] * b[3][2] + a[3][0] * b[3][3]
        val v31 = a[0][1] * b[3][0] + a[1][1] * b[3][1] + a[2][1] * b[3][2] + a[3][1] * b[3][3]
        val v32 = a[0][2] * b[3][0] + a[1][2] * b[3][1] + a[2][2] * b[3][2] + a[3][2] * b[3][3]
        val v33 = a[0][3] * b[3][0] + a[1][3] * b[3][1] + a[2][3] * b[3][2] + a[3][3] * b[3][3]

        res[0][0] = v00
        res[0][1] = v01
        res[0][2] = v02
        res[0][3] = v03
        res[1][0] = v10
        res[1][1] = v11
        res[1][2] = v12
        res[1][3] = v13
        res[2][0] = v20
        res[2][1] = v21
        res[2][2] = v22
        res[2][3] = v23
        res[3][0] = v30
        res[3][1] = v31
        res[3][2] = v32
        res[3][3] = v33

        return res
    }


    fun toByteBuffer(dbb: ByteBuffer, offset: Int) {
        dbb.putFloat(offset + 0 * Float.SIZE_BYTES, columns[0][0])
        dbb.putFloat(offset + 1 * Float.SIZE_BYTES, columns[0][1])
        dbb.putFloat(offset + 2 * Float.SIZE_BYTES, columns[0][2])
        dbb.putFloat(offset + 3 * Float.SIZE_BYTES, columns[0][3])
        dbb.putFloat(offset + 4 * Float.SIZE_BYTES, columns[1][0])
        dbb.putFloat(offset + 5 * Float.SIZE_BYTES, columns[1][1])
        dbb.putFloat(offset + 6 * Float.SIZE_BYTES, columns[1][2])
        dbb.putFloat(offset + 7 * Float.SIZE_BYTES, columns[1][3])
        dbb.putFloat(offset + 8 * Float.SIZE_BYTES, columns[2][0])
        dbb.putFloat(offset + 9 * Float.SIZE_BYTES, columns[2][1])
        dbb.putFloat(offset + 10 * Float.SIZE_BYTES, columns[2][2])
        dbb.putFloat(offset + 11 * Float.SIZE_BYTES, columns[2][3])
        dbb.putFloat(offset + 12 * Float.SIZE_BYTES, columns[3][0])
        dbb.putFloat(offset + 13 * Float.SIZE_BYTES, columns[3][1])
        dbb.putFloat(offset + 14 * Float.SIZE_BYTES, columns[3][2])
        dbb.putFloat(offset + 15 * Float.SIZE_BYTES, columns[3][3])
    }
}