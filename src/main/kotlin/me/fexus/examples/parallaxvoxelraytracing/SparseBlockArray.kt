package me.fexus.examples.parallaxvoxelraytracing


class SparseBlockArray(val extent: Int) {
    private val size = extent * extent * extent
    private val ranges = mutableListOf<BlockRange>(BlockRange(0, size, 0))


    fun get(x: Int, y: Int, z: Int): Int {
        val index = z * extent * extent + y * extent + x
        return ranges.first { it.start <= index && it.end > index }.block
    }

    fun set(x: Int, y: Int, z: Int, block: Int) {
        val index = z * extent * extent + y * extent + x
        val targetRange = ranges.first { it.start <= index && it.end > index }
        if (targetRange.block == block) return
        ranges.remove(targetRange)

        if (targetRange.start == index) {
            val prevRange = ranges.first { it.start <= index - 1 && it.end < index - 1 }
            val newPrevRange = if (prevRange.block == block) {
                ranges.remove(prevRange)
                BlockRange(prevRange.start, targetRange.start + 1, block)
            } else
                BlockRange(targetRange.start, targetRange.start + 1, block)
            val newNextRange = BlockRange(targetRange.start + 1, targetRange.end, targetRange.block)
            ranges.add(newPrevRange)
            ranges.add(newNextRange)
            return
        }

        if (targetRange.end - 1 == index) {
            val nextRange = ranges.first { it.start <= index + 1 && it.end < index + 1 }
            val newPreviousRange = BlockRange(targetRange.start, targetRange.end - 1, targetRange.block)
            val newNextRange = if (nextRange.block == block) {
                ranges.remove(nextRange)
                BlockRange(targetRange.end, nextRange.end, block)
            } else {
                BlockRange(index, index + 1, block)
            }
            ranges.add(newPreviousRange)
            ranges.add(newNextRange)
            return
        }

        val newPreviousRange = BlockRange(targetRange.start, index, targetRange.block)
        val newCenterRange = BlockRange(index, index + 1, block)
        val newNextRange = BlockRange(index + 1, targetRange.end, targetRange.block)
        ranges.add(newPreviousRange)
        ranges.add(newCenterRange)
        ranges.add(newNextRange)
    }

    // Non-inclusive end
    private class BlockRange(val start: Int, val end: Int, val block: Int)
}