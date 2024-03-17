package me.fexus.examples.surroundsound

import me.fexus.examples.surroundsound.ground.GroundHeightmap
import me.fexus.examples.surroundsound.ground.GroundMeshBuilder
import me.fexus.math.fract
import me.fexus.math.lerp
import me.fexus.math.vec.Vec3
import kotlin.jvm.internal.Ref.FloatRef
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt


class Ground(val width: Int, val breadth: Int) {
    private val heightmap = GroundHeightmap(width, breadth)
    private val meshBuilder = GroundMeshBuilder()
    lateinit var currentMesh: Mesh; private set


    fun generateNewHeightMap(seed: Long) {
        heightmap.reset()

        for (x in 0 until width) {
            for (z in 0 until breadth) {
                val rand = seed xor (((x * 32325.21f).roundToInt() - (z * 2332.244f).roundToInt()).toLong() * 0xFFFF * x * z)
                val height = (rand % 0.312536f) - 0.4f
                heightmap[x, z] = height
            }
        }
    }

    fun buildMesh(): Mesh {
        this.currentMesh = meshBuilder.createNewGroundMesh(heightmap)
        return this.currentMesh
    }

    // Sucks
    fun getHeightAt(x: Float, z: Float): Float {
        if (x.roundToInt() !in IntRange(0, width)) throw Exception("meh")
        if (z.roundToInt() !in IntRange(0, breadth)) throw Exception("meh")

        val minX = floor(x).toInt()
        val maxX = ceil(x).toInt()
        val minZ = floor(z).toInt()
        val maxZ = ceil(z).toInt()

        val pointA = Vec3(minX, heightmap[minX, minZ], minZ)
        val pointB = Vec3(maxX, heightmap[maxX, minZ], minZ)
        val pointC = Vec3(minX, heightmap[minX, maxZ], maxZ)
        val pointD = Vec3(maxX, heightmap[maxX, maxZ], maxZ)

        val progressX = fract(x)
        val progressZ = fract(z)

        val ab = lerp(pointA, pointB, progressX)
        val cd = lerp(pointC, pointD, progressX)

        val targetPoint = lerp(ab, cd, progressZ)

        return targetPoint.y
    }
}