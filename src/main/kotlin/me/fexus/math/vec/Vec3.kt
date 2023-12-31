package me.fexus.math.vec

import me.fexus.math.inverseSqrt
import java.nio.ByteBuffer
import kotlin.math.roundToInt

data class Vec3(override var x: Float, override var y: Float, override var z: Float): TVec3<Float>() {
    constructor(s: Float): this(s,s,s)
    constructor(x: Number, y: Number, z: Number): this(x.toFloat(), y.toFloat(), z.toFloat())
    constructor(vec: TVec3<*>): this(vec.x.toFloat(), vec.y.toFloat(), vec.z.toFloat())

    override operator fun plus(other: TVec3<Float>): Vec3 = Vec3(this.x + other.x, this.y + other.y, this.z + other.z)
    override operator fun minus(other: TVec3<Float>): Vec3 = Vec3(this.x - other.x, this.y - other.y, this.z - other.z)
    override operator fun times(other: TVec3<Float>): Vec3 = Vec3(this.x * other.x, this.y * other.y, this.z * other.z)
    override operator fun div(other: TVec3<Float>): Vec3 = Vec3(this.x / other.x, this.y / other.y, this.z / other.z)

    override operator fun plus(other: Number): Vec3 {
        val num = other.toFloat()
        return Vec3(this.x + num, this.y + num, this.z + num)
    }
    override operator fun minus(other: Number): Vec3 {
        val num = other.toFloat()
        return Vec3(this.x - num, this.y - num, this.z - num)
    }
    override operator fun times(other: Number): Vec3 {
        val num = other.toFloat()
        return Vec3(this.x * num, this.y * num, this.z * num)
    }
    override operator fun div(other: Number): Vec3 {
        val num = other.toFloat()
        return Vec3(this.x / num, this.y / num, this.z / num)
    }

    override operator fun unaryMinus(): Vec3 = Vec3(-x, -y, -z)

    override fun dot(other: TVec3<Float>): Float = this.x * other.x + this.y * other.y + this.z * other.z

    fun roundToIVec3(): IVec3 {
        return IVec3(
            x.roundToInt(),
            y.roundToInt(),
            z.roundToInt()
        )
    }

    fun normalize(): Vec3 {
        val invSqrtDotThis = inverseSqrt(dot(this))
        val x = this.x * invSqrtDotThis
        val y = this.y * invSqrtDotThis
        val z = this.z * invSqrtDotThis
        return Vec3(x,y,z)
    }

    fun mod(mod: Int): Vec3 {
        return Vec3(
            x % mod,
            y % mod,
            z % mod
        )
    }

    fun mod(mod: Float): Vec3 {
        return Vec3(
            x % mod,
            y % mod,
            z % mod
        )
    }

    fun sqrt(): Vec3 {
        return Vec3(
            kotlin.math.sqrt(x),
            kotlin.math.sqrt(y),
            kotlin.math.sqrt(z)
        )
    }

    fun ceil(): Vec3 {
        return Vec3(
            kotlin.math.ceil(x), kotlin.math.ceil(y), kotlin.math.ceil(z)
        )
    }

    fun floor(): Vec3 {
        return Vec3(
            kotlin.math.floor(x),
            kotlin.math.floor(y),
            kotlin.math.floor(z),
        )
    }


    fun toByteBuffer(buf: ByteBuffer, offset: Int) {
        buf.putFloat(offset, x)
        buf.putFloat(offset + 4, y)
        buf.putFloat(offset + 8, z)
    }


    override fun toString(): String {
        return "[$x, $y, $z]"
    }
}
