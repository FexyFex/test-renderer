package me.fexus.examples.coolvoxelrendering.collision

import me.fexus.math.vec.DVec3
import me.fexus.math.vec.Vec3


// position is in the center
data class CollisionAABB(
    var position: DVec3,
    var extent: Vec3
)