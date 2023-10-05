package me.fexus.math.vec

import kotlin.math.sqrt


abstract class TVec2<T: Number>: Vec<T> {
    abstract var x: T
    abstract var y: T

    override val length: Float; get() = sqrt(this.dot(this).toFloat())

    abstract fun plus(other: TVec2<T>): TVec2<T>
    abstract fun minus(other: TVec2<T>): TVec2<T>
    abstract fun times(other: TVec2<T>): TVec2<T>
    abstract fun div(other: TVec2<T>): TVec2<T>

    abstract fun plus(other: Number): TVec2<T>
    abstract fun minus(other: Number): TVec2<T>
    abstract fun times(other: Number): TVec2<T>
    abstract fun div(other: Number): TVec2<T>

    abstract fun dot(other: TVec2<T>): T

    operator fun get(index: Int): T {
        return when (index) {
            0 -> x
            1 -> y
            else -> throw IndexOutOfBoundsException(index)
        }
    }

    operator fun set(index: Int, value: T) {
        when (index) {
            0 -> x = value
            1 -> y = value
            else -> throw IndexOutOfBoundsException(index)
        }
    }
}