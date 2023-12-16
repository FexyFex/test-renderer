package me.fexus.fexgui.util

import kotlin.math.roundToInt

class Color(val r: Float, val g: Float, val b: Float, val a: Float) {

    fun toInt(format: Format): Int {
        val ir = (r * 255).roundToInt()
        val ig = (g * 255).roundToInt()
        val ib = (b * 255).roundToInt()
        val ia = (a * 255).roundToInt()
        return (ir shl format.rPos) or (ig shl format.gPos) or (ib shl format.bPos) or (ia shl format.aPos)
    }


    enum class Format(val rPos: Int, val gPos: Int, val bPos: Int, val aPos: Int) {
        RGBA8(0, 8, 16, 24),
        BGRA8(16, 8, 0, 24)
    }

    companion object {
        val INVISIBLE = Color(0f, 0f, 0f, 0f)
        val BLACK = Color(0f, 0f, 0f, 1f)
        val WHITE = Color(1f, 1f, 1f, 1f)
        val RED = Color(1f, 0f, 0f, 1f)
        val GREEN = Color(0f, 1f, 0f, 1f)
        val BLUE = Color(0f, 0f, 1f, 1f)
    }
}