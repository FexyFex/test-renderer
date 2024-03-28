package me.fexus.math.vec

data class DVec2(override var x: Double, override var y: Double): TVec2<Double> {
    constructor(s: Double): this(s,s)

    override operator fun plus(other: TVec2<Double>): DVec2 = DVec2(this.x + other.x, this.y + other.y)
    override operator fun minus(other: TVec2<Double>): DVec2 = DVec2(this.x - other.x, this.y - other.y)
    override operator fun times(other: TVec2<Double>): DVec2 = DVec2(this.x * other.x, this.y * other.y)
    override operator fun div(other: TVec2<Double>): DVec2 = DVec2(this.x / other.x, this.y / other.y)

    override operator fun plus(other: Number): DVec2 {
        val num = other.toInt()
        return DVec2(this.x + num, this.y + num)
    }
    override operator fun minus(other: Number): DVec2 {
        val num = other.toInt()
        return DVec2(this.x - num, this.y - num)
    }
    override operator fun times(other: Number): DVec2 {
        val num = other.toInt()
        return DVec2(this.x * num, this.y * num)
    }
    override operator fun div(other: Number): DVec2 {
        val num = other.toInt()
        return DVec2(this.x / num, this.y / num)
    }

    override operator fun unaryMinus(): DVec2 = DVec2(-x, -y)

    override fun dot(other: TVec2<Double>): Double = this.x * other.x + this.y * other.y
}
