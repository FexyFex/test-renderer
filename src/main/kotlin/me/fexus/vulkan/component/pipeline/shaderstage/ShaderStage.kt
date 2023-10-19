package me.fexus.vulkan.component.pipeline.shaderstage

import org.lwjgl.vulkan.KHRRayTracingPipeline.VK_SHADER_STAGE_CLOSEST_HIT_BIT_KHR
import org.lwjgl.vulkan.KHRRayTracingPipeline.VK_SHADER_STAGE_RAYGEN_BIT_KHR
import org.lwjgl.vulkan.VK12


enum class ShaderStage(override val vkBits: Int): IShaderStage {
    VERTEX(VK12.VK_SHADER_STAGE_VERTEX_BIT),
    FRAGMENT(VK12.VK_SHADER_STAGE_FRAGMENT_BIT),
    BOTH(VK12.VK_SHADER_STAGE_VERTEX_BIT or VK12.VK_SHADER_STAGE_FRAGMENT_BIT),
    COMPUTE(VK12.VK_SHADER_STAGE_COMPUTE_BIT),
    RAYGEN(VK_SHADER_STAGE_RAYGEN_BIT_KHR),
    CLOSEST_HIT(VK_SHADER_STAGE_CLOSEST_HIT_BIT_KHR),
    ALL(VK12.VK_SHADER_STAGE_ALL);

    companion object {
        fun Int.toShaderStageString(): String {
            return enumValues<ShaderStage>().find { it.vkBits == this }?.name ?: "Unknown shader stage $this"
        }
    }
}