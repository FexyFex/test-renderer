package me.fexus.vulkan.raytracing.accelerationstructure

import org.lwjgl.vulkan.KHRAccelerationStructure.VK_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL_KHR
import org.lwjgl.vulkan.KHRAccelerationStructure.VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR

enum class AccelerationStructureType(val vkValue: Int) {
    BOTTOM_LEVEL(VK_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL_KHR),
    TOP_LEVEL(VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR)
}