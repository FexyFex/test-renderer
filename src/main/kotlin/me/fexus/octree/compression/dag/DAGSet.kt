package me.fexus.octree.compression.dag

import java.nio.ByteBuffer


data class DAGSet(val nodeBuffer: ByteBuffer, val bitsPerIndex: Int, val indexBuffer: ByteBuffer, val textureIndexBuffer: ByteBuffer)
