package me.fexus.math

import kotlin.math.sqrt


fun inverseSqrt(value: Double) = 1.0 / sqrt(value)
fun inverseSqrt(value: Float) = 1f / sqrt(value)

fun repeatSquared(times: Int, block: (x: Int, y: Int) -> Unit) {
    for (x in 0 until times) {
        for (y in 0 until times) {
            block(x,y)
        }
    }
}

fun repeatCubed(times: Int, block: (x: Int, y: Int, z: Int) -> Unit) {
    for (x in 0 until times) {
        for (y in 0 until times) {
            for (z in 0 until times) {
                block(x, y, z)
            }
        }
    }
}

fun repeat3D(timesX: Int, timesY: Int, timesZ: Int, block: (x: Int, y: Int, z: Int) -> Unit) {
    for (x in 0 until timesX) {
        for (y in 0 until timesY) {
            for (z in 0 until timesZ) {
                block(x, y, z)
            }
        }
    }
}

val Float.rad get() = Math.toRadians(this.toDouble()).toFloat()
val Double.rad get() = Math.toRadians(this)