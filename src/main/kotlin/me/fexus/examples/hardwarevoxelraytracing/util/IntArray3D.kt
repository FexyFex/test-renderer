package me.fexus.examples.hardwarevoxelraytracing.util

import me.fexus.math.vec.IVec3


class IntArray3D(private val extent: Int): Collection<Int> {
    override val size: Int = extent * extent * extent
    private val values = IntArray(size) { 0 }


    operator fun set(position: IVec3, value: Int) = set(position.x, position.y, position.z, value)
    operator fun set(x: Int, y: Int, z: Int, value: Int) {
        values[z * extent * extent + y * extent + x] = value
    }

    operator fun get(position: IVec3) = get(position.x, position.y, position.z)
    operator fun get(x: Int, y: Int, z: Int) = values[z * extent * extent + y * extent + x]


    override fun iterator(): Iterator<Int> {
        return IntArray3DIterator(values, 0, size - 1)
    }


    override fun contains(element: Int) = element in values
    override fun isEmpty(): Boolean = values.all { it == 0 }
    override fun containsAll(elements: Collection<Int>): Boolean = elements.all { it in values }


    fun forEachIndexed3D(action: (x: Int, y: Int, z: Int, value: Int) -> Unit) {
        repeat(extent) { z ->
            repeat(extent) { y ->
                repeat(extent) { x ->
                    val index = z * extent * extent + y * extent + x
                    action(x, y, z, values[index])
                }
            }
        }
    }


    private class IntArray3DIterator(private val arr: IntArray, start: Int, private val endInclusive: Int): Iterator<Int> {
        private var initValue: Int = start

        override fun hasNext(): Boolean {
            return initValue < endInclusive
        }

        override fun next(): Int {
            return arr[initValue++]
        }
    }
}