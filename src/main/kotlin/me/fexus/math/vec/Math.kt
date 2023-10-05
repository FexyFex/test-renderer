package me.fexus.math.vec

import kotlin.math.max
import kotlin.math.min

fun mix(a: Float, b: Float, interpolation: Float) = a + interpolation * (b - a)
fun mix(a: Double, b: Double, interpolation: Double) = a + interpolation * (b - a)

fun clamp(a: Float, min: Float, max: Float) = min(max(a, min), max)
fun clamp(a: Double, min: Double, max: Double) = min(max(a, min), max)