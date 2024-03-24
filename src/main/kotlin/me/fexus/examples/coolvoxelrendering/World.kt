package me.fexus.examples.coolvoxelrendering

import me.fexus.examples.coolvoxelrendering.world.Chunk
import me.fexus.math.vec.IVec3


class World {
    private val chunks = mutableMapOf<IVec3, Chunk>()
}