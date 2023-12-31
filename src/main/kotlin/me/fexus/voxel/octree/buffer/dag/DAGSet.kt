package me.fexus.voxel.octree.buffer.dag

import java.nio.ByteBuffer


data class DAGSet(val nodeBuffer: ByteBuffer, val bitsPerIndex: Int, val indexBuffer: ByteBuffer, val textureIndexBuffer: ByteBuffer)
