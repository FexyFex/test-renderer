package me.fexus.math.vec

import java.nio.ByteBuffer

data class IVec3(override var x: Int, override var y: Int, override var z: Int): TVec3<Int>() {
    constructor(s: Int): this(s,s,s)
    constructor(x: Number, y: Number, z: Number): this(x.toInt(), y.toInt(), z.toInt())

    override operator fun plus(other: TVec3<Int>): IVec3 = IVec3(this.x + other.x, this.y + other.y, this.z + other.z)
    override operator fun minus(other: TVec3<Int>): IVec3 = IVec3(this.x - other.x, this.y - other.y, this.z - other.z)
    override operator fun times(other: TVec3<Int>): IVec3 = IVec3(this.x * other.x, this.y * other.y, this.z * other.z)
    override operator fun div(other: TVec3<Int>): IVec3 = IVec3(this.x / other.x, this.y / other.y, this.z / other.z)

    override operator fun plus(other: Number): IVec3 {
        val num = other.toInt()
        return IVec3(this.x + num, this.y + num, this.z + num)
    }
    override operator fun minus(other: Number): IVec3 {
        val num = other.toInt()
        return IVec3(this.x - num, this.y - num, this.z - num)
    }
    override operator fun times(other: Number): IVec3 {
        val num = other.toInt()
        return IVec3(this.x * num, this.y * num, this.z * num)
    }
    override operator fun div(other: Number): IVec3 {
        val num = other.toInt()
        return IVec3(this.x / num, this.y / num, this.z / num)
    }

    override operator fun unaryMinus(): IVec3 = IVec3(-x, -y, -z)

    fun dot(other: TVec3<Float>): Float = this.x * other.x + this.y * other.y + this.z * other.z
    override fun dot(other: TVec3<Int>): Int = this.x * other.x + this.y * other.y + this.z * other.z


    fun mod(other: IVec3): IVec3 {
        return IVec3(
            this.x % other.x,
            this.y % other.y,
            this.z % other.z
        )
    }


    fun floorMod(other: IVec3): IVec3 {
        return IVec3(
            Math.floorMod(this.x, other.x),
            Math.floorMod(this.y, other.y),
            Math.floorMod(this.z, other.z)
        )
    }


    fun toByteBuffer(buf: ByteBuffer, offset: Int) {
        buf.putInt(offset, x)
        buf.putInt(offset + 4, y)
        buf.putInt(offset + 8, z)
    }


    override fun toString(): String {
        return "[$x, $y, $z]"
    }
}
