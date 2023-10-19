package me.fexus.vulkan.raytracing.accelerationstructure

import me.fexus.math.mat.Mat4
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer


data class TopLevelAccelerationStructureConfiguration(
        val transformMatrix: Mat4,
        val instanceDataBuffer: VulkanBuffer,
        val bottomLevelAccelerationStructure: BottomLevelAccelerationStructure,
)
