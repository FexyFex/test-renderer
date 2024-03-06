package me.fexus.math

import me.fexus.math.vec.Vec3
import kotlin.math.*


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

val Float.rad get() = Math.toRadians(this.toDouble()).toFloat()
val Double.rad get() = Math.toRadians(this)


fun fract(value: Float): Float {
    return value - floor(value)
}


fun Float.clamp(min: Float, max: Float) = Math.clamp(this, min, max)
fun Double.clamp(min: Double, max: Double) = Math.clamp(this, min, max)


fun lerp(start: Vec3, end: Vec3, progress: Float): Vec3 {
    val distance = end - start
    val distanceCovered = distance * progress
    return start + distanceCovered
}

fun slerp(start: Vec3, end: Vec3, progress: Float): Vec3 {
    val dot = start.dot(end).clamp(-1.0f, 1.0f)

    val theta = acos(dot) * progress
    val relativeVec = (end - (start * dot)).normalize()

    return ((start * cos(theta)) + (relativeVec * sin(theta)))
}