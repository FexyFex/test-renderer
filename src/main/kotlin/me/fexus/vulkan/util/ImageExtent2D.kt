package me.fexus.vulkan.util

import me.fexus.math.vec.IVec2

data class ImageExtent2D(val width: Int, val height: Int) {
    constructor(ivec2: IVec2): this(ivec2.x, ivec2.y)
}
