package me.fexus.examples.hardwarevoxelraytracing.accelerationstructure

import me.fexus.vulkan.descriptors.buffer.VulkanBuffer


data class AABBTLASConfiguration(val instancesBuffers: List<VulkanBuffer>)