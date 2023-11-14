package me.fexus.math

import me.fexus.math.vec.IVec3
import me.fexus.math.vec.Vec3


fun random(st: IVec3): Float {
    return fract(kotlin.math.sin(st.dot(Vec3(12.9898f, 78.233f, 65.32111f)) * 43758.5453123f))
}