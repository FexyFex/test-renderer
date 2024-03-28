package me.fexus.math.vec

import me.fexus.math.clamp
import java.nio.ByteBuffer
import kotlin.math.absoluteValue


open class IVec3(override var x: Int, override var y: Int, override var z: Int): TVec3<Int> {
    constructor(s: Int): this(s,s,s)
    constructor(x: Number, y: Number, z: Number): this(x.toInt(), y.toInt(), z.toInt())
    constructor(v2: IVec2, z: Int): this(v2.x, v2.y, z)

    val abs: IVec3; get() = IVec3(x.absoluteValue, y.absoluteValue, z.absoluteValue)

    val xy: IVec2; get() = IVec2(x, y)

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


    fun clamp(min: IVec3, max: IVec3) = IVec3(
        x.clamp(min.x, max.x),
        y.clamp(min.y, max.y),
        z.clamp(min.z, max.z)
    )

    fun clamp(min: Int, max: Int) = IVec3(
        x.clamp(min, max),
        y.clamp(min, max),
        z.clamp(min, max)
    )


    fun intoByteBuffer(buf: ByteBuffer, offset: Int) {
        buf.putInt(offset, x)
        buf.putInt(offset + 4, y)
        buf.putInt(offset + 8, z)
    }


    fun any(predicate: (Int) -> Boolean) = predicate(x) || predicate(y) || predicate(z)

    fun all(predicate: (Int) -> Boolean) = predicate(x) && predicate(y) && predicate(z)

    fun indexOfFirst(predicate: (Int) -> Boolean): Int {
        if (predicate(x)) return 0
        if (predicate(y)) return 1
        if (predicate(z)) return 2
        throw Exception()
    }

    infix fun ushr(value: Int): IVec3 {
        return IVec3(x ushr value, y ushr value, z ushr value)
    }
    infix fun shr(value: Int): IVec3 {
        return IVec3(x shr value, y shr value, z shr value)
    }
    infix fun shl(value: Int): IVec3 {
        return IVec3(x shl value, y shl value, z shl value)
    }


    override fun toString(): String {
        return "[$x, $y, $z]"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is IVec3) return false
        return this.x == other.x && this.y == other.y && this.z == other.z
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }
}
