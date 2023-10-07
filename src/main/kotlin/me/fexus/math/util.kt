package me.fexus.math

import kotlin.math.sqrt


fun inverseSqrt(value: Double) = 1.0 / sqrt(value)
fun inverseSqrt(value: Float) = 1f / sqrt(value)

fun repeatSqaued(size: Int, block: (x: Int, y: Int) -> Unit) {
    for (x in 0 until size) {
        for (y in 0 until size) {
            block(x,y)
        }
    }
}

val Float.rad get() = Math.toRadians(this.toDouble()).toFloat()
val Double.rad get() = Math.toRadians(this)