package me.fexus.examples.compute.bulletlimbo

import me.fexus.examples.Globals
import me.fexus.math.mat.Mat4
import me.fexus.math.vec.Vec2
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlag
import java.nio.ByteBuffer
import java.nio.ByteOrder


class WorldInfoBuffer {
    private lateinit var deviceUtil: VulkanDeviceUtil
    private lateinit var buffers: Array<VulkanBuffer>


    fun init(deviceUtil: VulkanDeviceUtil) {
        this.deviceUtil = deviceUtil

        val cameraBufferLayout = VulkanBufferConfiguration(
            256L,
            MemoryPropertyFlag.HOST_VISIBLE + MemoryPropertyFlag.HOST_COHERENT,
            BufferUsage.UNIFORM_BUFFER
        )
        this.buffers = Array(Globals.FRAMES_TOTAL) { deviceUtil.createBuffer(cameraBufferLayout) }
    }

    operator fun get(index: Int) = buffers[index]

    fun updateData(camPos: Vec2, camExtent: Vec2, tickCounter: Long, playArea: Area2D, frameIndex: Int) {
        val data = ByteBuffer.allocate(128)
        data.order(ByteOrder.LITTLE_ENDIAN)
        camPos.toByteBuffer(data, 0)
        camExtent.toByteBuffer(data, 8)
        data.putInt(16, tickCounter.toInt())
        playArea.toByteBuffer(data, 20)
        buffers[frameIndex].put(0, data)
    }


    fun destroy() {
        buffers.forEach(VulkanBuffer::destroy)
    }
}