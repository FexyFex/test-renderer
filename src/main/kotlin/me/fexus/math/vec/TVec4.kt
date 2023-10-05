package me.fexus.math.vec

import kotlin.math.sqrt


abstract class TVec4<T: Number>: Vec<T> {
    abstract var x: T
    abstract var y: T
    abstract var z: T
    abstract var w: T

    var r: T; get() = x; set(value) { x = value }
    var g: T; get() = y; set(value) { y = value }
    var b: T; get() = z; set(value) { z = value }
    var a: T; get() = w; set(value) { w = value }

    override val length: Float; get() = sqrt(this.dot(this).toFloat())

    abstract fun plus(other: TVec4<T>): TVec4<T>
    abstract fun minus(other: TVec4<T>): TVec4<T>
    abstract fun times(other: TVec4<T>): TVec4<T>
    abstract fun div(other: TVec4<T>): TVec4<T>

    abstract fun plus(other: Number): TVec4<T>
    abstract fun minus(other: Number): TVec4<T>
    abstract fun times(other: Number): TVec4<T>
    abstract fun div(other: Number): TVec4<T>

    abstract fun dot(other: TVec4<T>): T

    operator fun get(index: Int): T {
        return when (index) {
            0 -> x
            1 -> y
            2 -> z
            3 -> w
            else -> throw IndexOutOfBoundsException(index)
        }
    }

    operator fun set(index: Int, value: T) {
        when (index) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            3 -> w = value
            else -> throw IndexOutOfBoundsException(index)
        }
    }
}