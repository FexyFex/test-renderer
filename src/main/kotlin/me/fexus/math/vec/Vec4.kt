package me.fexus.math.vec

import java.nio.ByteBuffer

data class Vec4(override var x: Float, override var y: Float, override var z: Float, override var w: Float): TVec4<Float>() {
    constructor(s: Float): this(s,s,s,s)
    constructor(x: Number, y: Number, z: Number, w: Number): this(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    override operator fun plus(other: TVec4<Float>): Vec4 = Vec4(this.x + other.x, this.y + other.y, this.z + other.z, this.w + other.w)
    override operator fun minus(other: TVec4<Float>): Vec4 = Vec4(this.x - other.x, this.y - other.y, this.z - other.z, this.w - other.w)
    override operator fun times(other: TVec4<Float>): Vec4 = Vec4(this.x * other.x, this.y * other.y, this.z * other.z, this.w * other.w)
    override operator fun div(other: TVec4<Float>): Vec4 = Vec4(this.x / other.x, this.y / other.y, this.z / other.z, this.w / other.w)

    override operator fun plus(other: Number): Vec4 {
        val num = other.toFloat()
        return Vec4(this.x + num, this.y + num, this.z + num, this.w + num)
    }
    override operator fun minus(other: Number): Vec4 {
        val num = other.toFloat()
        return Vec4(this.x - num, this.y - num, this.z - num, this.w - num)
    }
    override operator fun times(other: Number): Vec4 {
        val num = other.toFloat()
        return Vec4(this.x * num, this.y * num, this.z * num, this.w * num)
    }
    override operator fun div(other: Number): Vec4 {
        val num = other.toFloat()
        return Vec4(this.x / num, this.y / num, this.z / num, this.w / num)
    }

    override operator fun unaryMinus(): Vec4 = Vec4(-x, -y, -z, -w)

    override fun dot(other: TVec4<Float>): Float = this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w


    fun toByteBuffer(buf: ByteBuffer, offset: Int) {
        buf.putFloat(offset, x)
        buf.putFloat(offset + 4, y)
        buf.putFloat(offset + 8, z)
        buf.putFloat(offset + 12, w)
    }


    companion object {
        const val SIZE_BYTES = Float.SIZE_BYTES * 4
    }
}
