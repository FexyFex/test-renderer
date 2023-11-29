package me.fexus.math.vec

import java.nio.ByteBuffer

data class IVec2(override var x: Int, override var y: Int): TVec2<Int>() {
    constructor(s: Int): this(s,s)

    override operator fun plus(other: TVec2<Int>): IVec2 = IVec2(this.x + other.x, this.y + other.y)
    override operator fun minus(other: TVec2<Int>): IVec2 = IVec2(this.x - other.x, this.y - other.y)
    override operator fun times(other: TVec2<Int>): IVec2 = IVec2(this.x * other.x, this.y * other.y)
    override operator fun div(other: TVec2<Int>): IVec2 = IVec2(this.x / other.x, this.y / other.y)

    override operator fun plus(other: Number): IVec2 {
        val num = other.toInt()
        return IVec2(this.x + num, this.y + num)
    }
    override operator fun minus(other: Number): IVec2 {
        val num = other.toInt()
        return IVec2(this.x - num, this.y - num)
    }
    override operator fun times(other: Number): IVec2 {
        val num = other.toInt()
        return IVec2(this.x * num, this.y * num)
    }
    override operator fun div(other: Number): IVec2 {
        val num = other.toInt()
        return IVec2(this.x / num, this.y / num)
    }

    override operator fun unaryMinus(): IVec2 = IVec2(-x, -y)

    override fun dot(other: TVec2<Int>): Int = this.x * other.x + this.y * other.y


    fun toByteBuffer(buffer: ByteBuffer, offset: Int) {
        buffer.putInt(offset, x)
        buffer.putInt(offset + Int.SIZE_BYTES, y)
    }
}
