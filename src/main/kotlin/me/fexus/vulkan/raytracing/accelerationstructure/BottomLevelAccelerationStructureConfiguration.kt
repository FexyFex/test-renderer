package me.fexus.vulkan.raytracing.accelerationstructure

import me.fexus.vulkan.descriptors.buffer.VulkanBuffer

data class BottomLevelAccelerationStructureConfiguration(
        val primitivesCount: Int,
        val maxVertexIndex: Int,
        val vertexBuffer: VulkanBuffer,
        val indexBuffer: VulkanBuffer,
        val transformBuffer: VulkanBuffer
)
