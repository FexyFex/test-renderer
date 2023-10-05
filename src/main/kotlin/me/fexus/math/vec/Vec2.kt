package me.fexus.math.vec

data class Vec2(override var x: Float, override var y: Float): TVec2<Float>() {
    constructor(s: Float): this(s,s)

    override operator fun plus(other: TVec2<Float>): Vec2 = Vec2(this.x + other.x, this.y + other.y)
    override operator fun minus(other: TVec2<Float>): Vec2 = Vec2(this.x - other.x, this.y - other.y)
    override operator fun times(other: TVec2<Float>): Vec2 = Vec2(this.x * other.x, this.y * other.y)
    override operator fun div(other: TVec2<Float>): Vec2 = Vec2(this.x / other.x, this.y / other.y)

    override operator fun plus(other: Number): Vec2 {
        val num = other.toInt()
        return Vec2(this.x + num, this.y + num)
    }
    override operator fun minus(other: Number): Vec2 {
        val num = other.toInt()
        return Vec2(this.x - num, this.y - num)
    }
    override operator fun times(other: Number): Vec2 {
        val num = other.toInt()
        return Vec2(this.x * num, this.y * num)
    }
    override operator fun div(other: Number): Vec2 {
        val num = other.toInt()
        return Vec2(this.x / num, this.y / num)
    }

    override operator fun unaryMinus(): Vec2 = Vec2(-x, -y)

    override fun dot(other: TVec2<Float>): Float = this.x * other.x + this.y * other.y
}
