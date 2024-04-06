package me.fexus.examples.coolvoxelrendering.world.position

import me.fexus.math.vec.Vec3


class ChunkLocalPosition(x: Float, y: Float, z: Float): Vec3(x,y,z) {
    constructor(x: Number, y: Number, z: Number): this(x.toFloat(), y.toFloat(), z.toFloat())
    constructor(pos: Vec3): this(pos.x, pos.y, pos.z)
}