package me.fexus.vulkan.component.pipeline

import org.lwjgl.vulkan.VK10

enum class ShaderStage(val vkBits: Int) {
    NONE(0),
    VERTEX(VK10.VK_SHADER_STAGE_VERTEX_BIT),
    FRAGMENT(VK10.VK_SHADER_STAGE_FRAGMENT_BIT),
    BOTH(VK10.VK_SHADER_STAGE_VERTEX_BIT or VK10.VK_SHADER_STAGE_FRAGMENT_BIT),
    COMPUTE(VK10.VK_SHADER_STAGE_COMPUTE_BIT),
    ALL(VK10.VK_SHADER_STAGE_ALL);


    companion object {
        fun Int.toShaderStageString(): String {
            return enumValues<ShaderStage>().find { it.vkBits == this }?.name ?: "Unknown shader stage $this"
        }
    }
}