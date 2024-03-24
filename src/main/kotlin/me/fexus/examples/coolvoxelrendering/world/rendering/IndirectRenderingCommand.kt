package me.fexus.examples.coolvoxelrendering.world.rendering

import java.nio.ByteBuffer

data class IndirectRenderingCommand(
    val vertexCount: Int,
    val instanceCount: Int,
    val firstVertex: Int,
    val firstInstance: Int
) {

    fun intoByteBuffer(buf: ByteBuffer, offset: Int) {

    }
}