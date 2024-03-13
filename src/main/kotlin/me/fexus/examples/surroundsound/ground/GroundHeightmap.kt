package me.fexus.examples.surroundsound.ground

class GroundHeightmap(val width: Int, val breadth: Int) {
    private val heights = FloatArray(width * breadth) { 0f }


    init {
        if (width <= 1 || breadth <= 1) {
            throw Exception("GroundHeightmap dimensions must not be lower than 2")
        }
    }

    operator fun set(x: Int, z: Int, value: Float) {
        heights[x + z * width] = value
    }
    operator fun get(x: Int, z: Int) = heights[x + z * width]


    fun reset() {
        heights.fill(0f)
    }
}