package me.fexus.audio

import me.fexus.math.vec.DVec3
import me.fexus.math.vec.Vec3

data class ListenerData(
    val position: DVec3,
    val rotation: Vec3,
    val up: Vec3,
    val velocity: Vec3
)
