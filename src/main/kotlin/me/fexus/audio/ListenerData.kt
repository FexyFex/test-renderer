package me.fexus.audio

import me.fexus.math.vec.Vec3

data class ListenerData(
    val position: Vec3,
    val rotation: Vec3,
    val up: Vec3,
    val velocity: Vec3
)
