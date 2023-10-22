package me.fexus.vulkan.raytracing.accelerationstructure

import me.fexus.vulkan.descriptors.buffer.VulkanBuffer


data class TopLevelAccelerationStructureConfiguration(
        val instanceDataBuffer: VulkanBuffer,
        val bottomLevelAccelerationStructure: BottomLevelAccelerationStructure,
)
