package me.fexus.examples.hardwarevoxelraytracing.octree.position

// All params here will either be 0 or 1. An Octant is an 8th of a whole, much like a quadrant.
// I came up with that word, not sure if it exists...
data class OctantPosition(override val x: Int, override val y: Int, override val z: Int): IOctreePosition
