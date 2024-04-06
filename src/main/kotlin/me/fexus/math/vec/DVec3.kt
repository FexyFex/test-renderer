package me.fexus.math.vec

import kotlin.math.absoluteValue

open class DVec3(override var x: Double, override var y: Double, override var z: Double): TVec3<Double> {
    constructor(s: Double): this(s,s,s)
    constructor(vec: TVec3<Number>): this(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble())
    constructor(vec3: DVec3): this(vec3.x, vec3.y, vec3.z)
    constructor(vec3: Vec3): this(vec3.x.toDouble(), vec3.y.toDouble(), vec3.z.toDouble())
    constructor(x: Number, y: Number, z: Number): this(x.toDouble(), y.toDouble(), z.toDouble())

    override val abs: DVec3
        get() = DVec3(this.x.absoluteValue, this.y.absoluteValue, this.z.absoluteValue)


    override operator fun plus(other: TVec3<Double>): DVec3 = DVec3(this.x + other.x, this.y + other.y, this.z + other.z)
    override operator fun minus(other: TVec3<Double>): DVec3 = DVec3(this.x - other.x, this.y - other.y, this.z - other.z)
    override operator fun times(other: TVec3<Double>): DVec3 = DVec3(this.x * other.x, this.y * other.y, this.z * other.z)
    override operator fun div(other: TVec3<Double>): DVec3 = DVec3(this.x / other.x, this.y / other.y, this.z / other.z)

    override operator fun plus(other: Number): DVec3 {
        val num = other.toFloat()
        return DVec3(this.x + num, this.y + num, this.z + num)
    }
    override operator fun minus(other: Number): DVec3 {
        val num = other.toFloat()
        return DVec3(this.x - num, this.y - num, this.z - num)
    }
    override operator fun times(other: Number): DVec3 {
        val num = other.toFloat()
        return DVec3(this.x * num, this.y * num, this.z * num)
    }
    override operator fun div(other: Number): DVec3 {
        val num = other.toFloat()
        return DVec3(this.x / num, this.y / num, this.z / num)
    }

    override operator fun unaryMinus(): DVec3 = DVec3(-x, -y, -z)

    override fun dot(other: TVec3<Double>): Double = this.x * other.x + this.y * other.y + this.z * other.z


    operator fun plus(other: TVec3<Float>): DVec3 = DVec3(this.x + other.x, this.y + other.y, this.z + other.z)
    operator fun minus(other: TVec3<Float>): DVec3 = DVec3(this.x - other.x, this.y - other.y, this.z - other.z)
    operator fun times(other: TVec3<Float>): DVec3 = DVec3(this.x * other.x, this.y * other.y, this.z * other.z)
    operator fun div(other: TVec3<Float>): DVec3 = DVec3(this.x / other.x, this.y / other.y, this.z / other.z)


    override fun equals(other: Any?): Boolean {
        if (other !is DVec3) return false
        return this.x == other.x && this.y == other.y && this.z == other.z
    }
}
