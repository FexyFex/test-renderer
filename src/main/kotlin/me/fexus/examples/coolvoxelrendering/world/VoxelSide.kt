package me.fexus.examples.coolvoxelrendering.world

import me.fexus.math.vec.IVec2
import me.fexus.math.vec.IVec3


data class VoxelSide(
    val position: IVec3,
    val scale: IVec2,
    val direction: Direction,
    val textureIndex: Int
) {

    fun packToInt(): Int {
        return 0 or
                ((position.x and BITS_POSITION) shl BIT_OFFSET_POSITION_X) or
                ((position.y and BITS_POSITION) shl BIT_OFFSET_POSITION_Y) or
                ((position.z and BITS_POSITION) shl BIT_OFFSET_POSITION_Z) or
                ((scale.x and BITS_SCALING) shl BIT_OFFSET_SCALING_X) or
                ((scale.y and BITS_SCALING) shl BIT_OFFSET_SCALING_Y) or
                ((direction.value and BITS_DIRECTION) shl BIT_OFFSET_DIRECTION) or
                ((textureIndex and BITS_TEXTURE_INDEX) shl BIT_OFFSET_TEXTURE_INDEX)
    }


    companion object {
        private const val BITS_POSITION = 31 // 5 bits
        private const val BITS_SCALING = 31 // 5 bits
        private const val BITS_DIRECTION = 7 // 3 bits
        private const val BITS_TEXTURE_INDEX = 15 // 4 bits
        // (5 * 3) + (5 * 2) + 3 + 4 = 32

        private const val BIT_OFFSET_POSITION_X = 0
        private const val BIT_OFFSET_POSITION_Y = 5
        private const val BIT_OFFSET_POSITION_Z = 10
        private const val BIT_OFFSET_SCALING_X = 15
        private const val BIT_OFFSET_SCALING_Y = 20
        private const val BIT_OFFSET_DIRECTION = 25
        private const val BIT_OFFSET_TEXTURE_INDEX = 28

        const val SIZE_BYTES = Int.SIZE_BYTES
    }
}