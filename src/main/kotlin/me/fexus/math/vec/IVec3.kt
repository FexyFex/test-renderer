package me.fexus.math.vec

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

    override fun dot(other: TVec3<Int>): Int = this.x * other.x + this.y * other.y + this.z * other.z


}
