package me.fexus.math.vec

data class IVec4(override var x: Int, override var y: Int, override var z: Int, override var w: Int): TVec4<Int>() {
    constructor(s: Int): this(s,s,s,s)
    constructor(x: Number, y: Number, z: Number, w: Number): this(x.toInt(), y.toInt(), z.toInt(), w.toInt())

    override operator fun plus(other: TVec4<Int>): IVec4 = IVec4(this.x + other.x, this.y + other.y, this.z + other.z, this.w + other.w)
    override operator fun minus(other: TVec4<Int>): IVec4 = IVec4(this.x - other.x, this.y - other.y, this.z - other.z, this.w - other.w)
    override operator fun times(other: TVec4<Int>): IVec4 = IVec4(this.x * other.x, this.y * other.y, this.z * other.z, this.w * other.w)
    override operator fun div(other: TVec4<Int>): IVec4 = IVec4(this.x / other.x, this.y / other.y, this.z / other.z, this.w / other.w)

    override operator fun plus(other: Number): IVec4 {
        val num = other.toInt()
        return IVec4(this.x + num, this.y + num, this.z + num, this.w + num)
    }
    override operator fun minus(other: Number): IVec4 {
        val num = other.toInt()
        return IVec4(this.x - num, this.y - num, this.z - num, this.w - num)
    }
    override operator fun times(other: Number): IVec4 {
        val num = other.toInt()
        return IVec4(this.x * num, this.y * num, this.z * num, this.w * num)
    }
    override operator fun div(other: Number): IVec4 {
        val num = other.toInt()
        return IVec4(this.x / num, this.y / num, this.z / num, this.w / num)
    }

    override operator fun unaryMinus(): IVec4 = IVec4(-x, -y, -z, -w)

    override fun dot(other: TVec4<Int>): Int = this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w
}
