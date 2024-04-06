package me.fexus.math.vec

import kotlin.math.sqrt


interface TVec3<T: Number>: Vec<T> {
    abstract var x: T
    abstract var y: T
    abstract var z: T

    var r: T; get() = x; set(value) { x = value }
    var g: T; get() = y; set(value) { y = value }
    var b: T; get() = z; set(value) { z = value }

    override val length: Float; get() = sqrt(this.dot(this).toFloat())
    val abs: TVec3<T>

    abstract fun plus(other: TVec3<T>): TVec3<T>
    abstract fun minus(other: TVec3<T>): TVec3<T>
    abstract fun times(other: TVec3<T>): TVec3<T>
    abstract fun div(other: TVec3<T>): TVec3<T>

    abstract fun plus(other: Number): TVec3<T>
    abstract fun minus(other: Number): TVec3<T>
    abstract fun times(other: Number): TVec3<T>
    abstract fun div(other: Number): TVec3<T>

    abstract fun dot(other: TVec3<T>): T

    operator fun get(index: Int): T {
        return when (index) {
            0 -> x
            1 -> y
            2 -> z
            else -> throw IndexOutOfBoundsException(index)
        }
    }

    operator fun set(index: Int, value: T) {
        when (index) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else -> throw IndexOutOfBoundsException(index)
        }
    }
}