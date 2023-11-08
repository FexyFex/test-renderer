package me.fexus.examples.hardwarevoxelraytracing.octree.compression

import java.nio.ByteBuffer


data class SVOSet(val nodeBuffer: ByteBuffer, val bitsPerIndex: Int, val indexBuffer: ByteBuffer, val textureIndexBuffer: ByteBuffer)
