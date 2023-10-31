package me.fexus.vulkan.raytracing

import org.lwjgl.vulkan.KHRRayTracingPipeline.*

enum class RaytracingShaderGroupType(val vkValue: Int) {
    GENERAL(VK_RAY_TRACING_SHADER_GROUP_TYPE_GENERAL_KHR),
    TRIANGLES_HIT(VK_RAY_TRACING_SHADER_GROUP_TYPE_TRIANGLES_HIT_GROUP_KHR),
    PROCEDURAL_HIT(VK_RAY_TRACING_SHADER_GROUP_TYPE_PROCEDURAL_HIT_GROUP_KHR)

}