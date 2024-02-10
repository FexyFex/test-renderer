package me.fexus.examples.compute

import me.fexus.math.vec.Vec2
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import java.nio.ByteBuffer


data class ParticleInitialData(
        val spawnPosition: Vec2,
        val spawnTimeStamp: Long, // treated as a uint
        val behaviourID: Int,
        val visualID: Int
) {

    fun intoByteBuffer(buf: ByteBuffer, offset: Int) {
        spawnPosition.toByteBuffer(buf, offset)
        buf.putInt(offset + 8, spawnTimeStamp.toInt())
        buf.putInt(offset + 12, behaviourID)
        buf.putInt(offset + 16, visualID)
    }

    fun moveIntoVulkanBuffer(buf: VulkanBuffer, offset: Int) {
        buf.putFloat(offset, spawnPosition.x)
        buf.putFloat(offset + 4, spawnPosition.y)
        buf.putInt(offset + 8, spawnTimeStamp.toInt())
        buf.putInt(offset + 12, behaviourID)
        buf.putInt(offset + 16, visualID)
    }

    companion object {
        const val SIZE_BYTES = 32
    }
}
