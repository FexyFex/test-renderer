package me.fexus.examples.hardwarevoxelraytracing.voxelanimation

import me.fexus.math.vec.IVec3
import me.fexus.math.vec.Vec4
import kotlin.math.log2


class VoxelColorGrid(val extent: Int) {
    val voxelCount: Int = extent * extent * extent
    private val log2 = log2(extent.toFloat())
    private val bounds = 0 until extent

    private val grid = Array<Vec4>(voxelCount) { Vec4(0f) }


    fun setVoxelAt(x: Int, y: Int, z: Int, color: Vec4) = setVoxelAt(IVec3(x,y,z), color)
    fun setVoxelAt(pos: IVec3, color: Vec4) {
        assertCoords(pos)
        grid[posToIndex(pos)] = color
    }

    fun clear() {
        grid.fill(Vec4(0f))
    }

    fun getVoxelAt(x: Int, y: Int, z: Int) = getVoxelAt(IVec3(x,y,z))
    fun getVoxelAt(pos: IVec3): Vec4 {
        assertCoords(pos)
        return grid[posToIndex(pos)]
    }

    private fun posToIndex(x: Int, y: Int, z: Int): Int = posToIndex(IVec3(x,y,z))
    private fun posToIndex(pos: IVec3): Int {
        return pos.x + (pos.y * extent) + (pos.z * extent * extent)
    }

    private fun assertCoords(pos: IVec3) = assertCoords(pos.x, pos.y, pos.z)
    private fun assertCoords(x: Int, y: Int, z: Int) {
        if (x !in bounds || y !in bounds || z !in bounds)
            throw AssertionError("Out of bounds $x, $y, $z")
    }


    fun forEachFilledVoxel(action: (x: Int, y: Int, z: Int, color: Vec4) -> Unit) {
        repeat(extent) { z ->
            repeat(extent) { y ->
                repeat(extent) innermost@ { x ->
                    val v = getVoxelAt(x,y,z)
                    if (v.w <= 0f) return@innermost
                    action(x, y, z, v)
                }
            }
        }
    }


    companion object {
        private fun Boolean.toInt() = if(this) 1 else 0
    }
}