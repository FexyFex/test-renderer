package me.fexus.math.mat

import me.fexus.math.inverseSqrt
import me.fexus.math.quat.Quat
import me.fexus.math.repeatSquared
import me.fexus.math.vec.Vec3
import me.fexus.math.vec.Vec4
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


// column major
class Mat4(val rows: Array<Vec4>) {
    constructor(): this(1f)
    constructor(s: Float): this(s,s,s,s)
    constructor(x: Float, y: Float, z: Float, w: Float): this(
        arrayOf(
            Vec4(x, 0f, 0f, 0f),
            Vec4(0f, y, 0f, 0f),
            Vec4(0f, 0f, z, 0f),
            Vec4(0f, 0f, 0f, w)
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

    constructor(other: Mat4): this(
        other.x.x,other.x.y,other.x.z,other.x.w,
        other.y.x,other.y.y,other.y.z,other.y.w,
        other.z.x,other.z.y,other.z.z,other.z.w,
        other.w.x,other.w.y,other.w.z,other.w.w,
    )

    var x: Vec4; get() = rows[0]; set(value) = rows.set(0, value)
    var y: Vec4; get() = rows[1]; set(value) = rows.set(1, value)
    var z: Vec4; get() = rows[2]; set(value) = rows.set(2, value)
    var w: Vec4; get() = rows[3]; set(value) = rows.set(3, value)

    operator fun get(index: Int) = rows[index]
    operator fun get(columnIndex: Int, rowIndex: Int) = rows[columnIndex][rowIndex]

    fun put(other: Mat4) {
        repeatSquared(4) { x, y ->
            this.rows[x][y] = other.rows[x][y]
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

    operator fun times(other: Vec4): Vec4 {
        val a = this
        val res = Vec4()

        val x = a[0][0] * other.x + a[1][0] * other.y + a[2][0] * other.z + a[3][0] * other.w
        val y = a[0][1] * other.x + a[1][1] * other.y + a[2][1] * other.z + a[3][1] * other.w
        val z = a[0][2] * other.x + a[1][2] * other.y + a[2][2] * other.z + a[3][2] * other.w
        val w = a[0][3] * other.x + a[1][3] * other.y + a[2][3] * other.z + a[3][3] * other.w
        res.x = x
        res.y = y
        res.z = z
        res.w = w
        return res
    }

    operator fun div(other: Mat4): Mat4 {
        return other.inverse() * this
    }


    fun toByteBufferColumnMajor(buf: ByteBuffer, offset: Int) {
        buf.putFloat(offset + 0 * Float.SIZE_BYTES, rows[0][0])
        buf.putFloat(offset + 1 * Float.SIZE_BYTES, rows[0][1])
        buf.putFloat(offset + 2 * Float.SIZE_BYTES, rows[0][2])
        buf.putFloat(offset + 3 * Float.SIZE_BYTES, rows[0][3])
        buf.putFloat(offset + 4 * Float.SIZE_BYTES, rows[1][0])
        buf.putFloat(offset + 5 * Float.SIZE_BYTES, rows[1][1])
        buf.putFloat(offset + 6 * Float.SIZE_BYTES, rows[1][2])
        buf.putFloat(offset + 7 * Float.SIZE_BYTES, rows[1][3])
        buf.putFloat(offset + 8 * Float.SIZE_BYTES, rows[2][0])
        buf.putFloat(offset + 9 * Float.SIZE_BYTES, rows[2][1])
        buf.putFloat(offset + 10 * Float.SIZE_BYTES, rows[2][2])
        buf.putFloat(offset + 11 * Float.SIZE_BYTES, rows[2][3])
        buf.putFloat(offset + 12 * Float.SIZE_BYTES, rows[3][0])
        buf.putFloat(offset + 13 * Float.SIZE_BYTES, rows[3][1])
        buf.putFloat(offset + 14 * Float.SIZE_BYTES, rows[3][2])
        buf.putFloat(offset + 15 * Float.SIZE_BYTES, rows[3][3])
    }


    fun toByteBufferRowMajor(buf: ByteBuffer, offset: Int) {
        buf.putFloat(offset + 0 * Float.SIZE_BYTES, rows[0][0])
        buf.putFloat(offset + 1 * Float.SIZE_BYTES, rows[1][0])
        buf.putFloat(offset + 2 * Float.SIZE_BYTES, rows[2][0])
        buf.putFloat(offset + 3 * Float.SIZE_BYTES, rows[3][0])
        buf.putFloat(offset + 4 * Float.SIZE_BYTES, rows[0][1])
        buf.putFloat(offset + 5 * Float.SIZE_BYTES, rows[1][1])
        buf.putFloat(offset + 6 * Float.SIZE_BYTES, rows[2][1])
        buf.putFloat(offset + 7 * Float.SIZE_BYTES, rows[3][1])
        buf.putFloat(offset + 8 * Float.SIZE_BYTES, rows[0][2])
        buf.putFloat(offset + 9 * Float.SIZE_BYTES, rows[1][2])
        buf.putFloat(offset + 10 * Float.SIZE_BYTES, rows[2][2])
        buf.putFloat(offset + 11 * Float.SIZE_BYTES, rows[3][2])
        buf.putFloat(offset + 12 * Float.SIZE_BYTES, rows[0][3])
        buf.putFloat(offset + 13 * Float.SIZE_BYTES, rows[1][3])
        buf.putFloat(offset + 14 * Float.SIZE_BYTES, rows[2][3])
        buf.putFloat(offset + 15 * Float.SIZE_BYTES, rows[3][3])
    }


    fun inverse(): Mat4 {
        val m = Mat4(1f)
        m.put(this)
        val res = Mat4(1f)

        val c00 = m[2][2] * m[3][3] - m[3][2] * m[2][3]
        val c02 = m[1][2] * m[3][3] - m[3][2] * m[1][3]
        val c03 = m[1][2] * m[2][3] - m[2][2] * m[1][3]

        val c04 = m[2][1] * m[3][3] - m[3][1] * m[2][3]
        val c06 = m[1][1] * m[3][3] - m[3][1] * m[1][3]
        val c07 = m[1][1] * m[2][3] - m[2][1] * m[1][3]

        val c08 = m[2][1] * m[3][2] - m[3][1] * m[2][2]
        val c10 = m[1][1] * m[3][2] - m[3][1] * m[1][2]
        val c11 = m[1][1] * m[2][2] - m[2][1] * m[1][2]

        val c12 = m[2][0] * m[3][3] - m[3][0] * m[2][3]
        val c14 = m[1][0] * m[3][3] - m[3][0] * m[1][3]
        val c15 = m[1][0] * m[2][3] - m[2][0] * m[1][3]

        val c16 = m[2][0] * m[3][2] - m[3][0] * m[2][2]
        val c18 = m[1][0] * m[3][2] - m[3][0] * m[1][2]
        val c19 = m[1][0] * m[2][2] - m[2][0] * m[1][2]

        val c20 = m[2][0] * m[3][1] - m[3][0] * m[2][1]
        val c22 = m[1][0] * m[3][1] - m[3][0] * m[1][1]
        val c23 = m[1][0] * m[2][1] - m[2][0] * m[1][1]

        val i00 = +(m[1][1] * c00 - m[1][2] * c04 + m[1][3] * c08)
        val i01 = -(m[0][1] * c00 - m[0][2] * c04 + m[0][3] * c08)
        val i02 = +(m[0][1] * c02 - m[0][2] * c06 + m[0][3] * c10)
        val i03 = -(m[0][1] * c03 - m[0][2] * c07 + m[0][3] * c11)

        val i10 = -(m[1][0] * c00 - m[1][2] * c12 + m[1][3] * c16)
        val i11 = +(m[0][0] * c00 - m[0][2] * c12 + m[0][3] * c16)
        val i12 = -(m[0][0] * c02 - m[0][2] * c14 + m[0][3] * c18)
        val i13 = +(m[0][0] * c03 - m[0][2] * c15 + m[0][3] * c19)

        val i20 = +(m[1][0] * c04 - m[1][1] * c12 + m[1][3] * c20)
        val i21 = -(m[0][0] * c04 - m[0][1] * c12 + m[0][3] * c20)
        val i22 = +(m[0][0] * c06 - m[0][1] * c14 + m[0][3] * c22)
        val i23 = -(m[0][0] * c07 - m[0][1] * c15 + m[0][3] * c23)

        val i30 = -(m[1][0] * c08 - m[1][1] * c16 + m[1][2] * c20)
        val i31 = +(m[0][0] * c08 - m[0][1] * c16 + m[0][2] * c20)
        val i32 = -(m[0][0] * c10 - m[0][1] * c18 + m[0][2] * c22)
        val i33 = +(m[0][0] * c11 - m[0][1] * c19 + m[0][2] * c23)

        val oneOverDet = 1 / (m[0][0] * i00 + m[0][1] * i10 + m[0][2] * i20 + m[0][3] * i30)

        res[0][0] = i00 * oneOverDet
        res[0][1] = i01 * oneOverDet
        res[0][2] = i02 * oneOverDet
        res[0][3] = i03 * oneOverDet

        res[1][0] = i10 * oneOverDet
        res[1][1] = i11 * oneOverDet
        res[1][2] = i12 * oneOverDet
        res[1][3] = i13 * oneOverDet

        res[2][0] = i20 * oneOverDet
        res[2][1] = i21 * oneOverDet
        res[2][2] = i22 * oneOverDet
        res[2][3] = i23 * oneOverDet

        res[3][0] = i30 * oneOverDet
        res[3][1] = i31 * oneOverDet
        res[3][2] = i32 * oneOverDet
        res[3][3] = i33 * oneOverDet

        return res
    }

    fun lookAt(eye: Vec3, center: Vec3, up: Vec3): Mat4 {
        val res = Mat4(1f)

        val ceX = center.x - eye.x
        val ceY = center.y - eye.y
        val ceZ = center.z - eye.z

        var dot = ceX * ceX + ceY * ceY + ceZ * ceZ
        var invSpqr = inverseSqrt(dot)
        val fX = ceX * invSpqr
        val fY = ceY * invSpqr
        val fZ = ceZ * invSpqr

        val fuX = fY * up.z - up.y * fZ
        val fuY = fZ * up.x - up.z * fX
        val fuZ = fX * up.y - up.x * fY
        dot = fuX * fuX + fuY * fuY + fuZ * fuZ
        invSpqr = inverseSqrt(dot)
        val sX = fuX * invSpqr
        val sY = fuY * invSpqr
        val sZ = fuZ * invSpqr

        val uX = sY * fZ - fY * sZ
        val uY = sZ * fX - fZ * sX
        val uZ = sX * fY - fX * sY

        res[0][0] = sX
        res[1][0] = sY
        res[2][0] = sZ
        res[0][1] = uX
        res[1][1] = uY
        res[2][1] = uZ
        res[0][2] = -fX
        res[1][2] = -fY
        res[2][2] = -fZ
        res[3][0] = -(sX * eye.x + sY * eye.y + sZ * eye.z)
        res[3][1] = -(uX * eye.x + uY * eye.y + uZ * eye.z)
        res[3][2] = fX * eye.x + fY * eye.y + fZ * eye.z

        return res
    }

    fun toQuat(): Quat {
        val res = Quat()

        val m00 = x.x
        val m01 = x.y
        val m02 = x.z
        val m10 = y.x
        val m11 = y.y
        val m12 = y.z
        val m20 = z.x
        val m21 = z.y
        val m22 = z.z

        val fourXSquaredMinus1 = m00 - m11 - m22
        val fourYSquaredMinus1 = m11 - m00 - m22
        val fourZSquaredMinus1 = m22 - m00 - m11
        val fourWSquaredMinus1 = m00 + m11 + m22

        var biggestIndex = 0
        var fourBiggestSquaredMinus1 = fourWSquaredMinus1
        if (fourXSquaredMinus1 > fourBiggestSquaredMinus1) {
            fourBiggestSquaredMinus1 = fourXSquaredMinus1
            biggestIndex = 1
        }
        if (fourYSquaredMinus1 > fourBiggestSquaredMinus1) {
            fourBiggestSquaredMinus1 = fourYSquaredMinus1
            biggestIndex = 2
        }
        if (fourZSquaredMinus1 > fourBiggestSquaredMinus1) {
            fourBiggestSquaredMinus1 = fourZSquaredMinus1
            biggestIndex = 3
        }

        val biggestVal = sqrt(fourBiggestSquaredMinus1 + 1f) * 0.5f
        val mult = 0.25f / biggestVal

        when (biggestIndex) {
            0 -> {
                res.w = biggestVal
                res.x = (m12 - m21) * mult
                res.y = (m20 - m02) * mult
                res.z = (m01 - m10) * mult
            }
            1 -> {
                res.w = (m12 - m21) * mult
                res.x = biggestVal
                res.y = (m01 + m10) * mult
                res.z = (m20 + m02) * mult
            }
            2 -> {
                res.w = (m20 - m02) * mult
                res.x = (m01 + m10) * mult
                res.y = biggestVal
                res.z = (m12 + m21) * mult
            }
            3 -> {
                res.w = (m01 - m10) * mult
                res.x = (m20 + m02) * mult
                res.y = (m12 + m21) * mult
                res.z = biggestVal
            }
            else -> throw Exception("How TF?")
        }

        return res
    }

    override fun toString(): String {
        return """
            [${x.x},${y.x},${z.x},${w.x}]
            [${x.y},${y.y},${z.y},${w.y}]
            [${x.z},${y.z},${z.z},${w.z}]
            [${x.w},${y.w},${z.w},${w.w}]
        """.trimIndent()
    }


    companion object {
        const val SIZE_BYTES = Vec4.SIZE_BYTES * 4
    }
}