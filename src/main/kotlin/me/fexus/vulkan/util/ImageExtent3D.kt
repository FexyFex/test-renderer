package me.fexus.vulkan.util

data class ImageExtent3D(val width: Int, val height: Int, val depth: Int) {
    constructor(extent2D: ImageExtent2D, depth: Int): this(extent2D.width, extent2D.height, depth)
}
