package me.fexus.math.vec

import kotlin.math.max
import kotlin.math.min

fun mix(a: Float, b: Float, interpolation: Float) = a + interpolation * (b - a)
fun mix(a: Double, b: Double, interpolation: Double) = a + interpolation * (b - a)

fun clamp(a: Float, min: Float, max: Float) = min(max(a, min), max)
fun clamp(a: Double, min: Double, max: Double) = min(max(a, min), max)

fun max(a: Vec3, b: Vec3) = Vec3(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z))
fun max(a: IVec3, b: IVec3) = IVec3(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z))

fun min(a: Vec3, b: Vec3) = Vec3(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z))
fun min(a: IVec3, b: IVec3) = IVec3(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z))