package me.fexus.vulkan.component.pipeline.shaderstage

interface IShaderStage {
    val vkBits: Int

    operator fun plus(other: IShaderStage): CombinedShaderStage {
        return CombinedShaderStage(this.vkBits or other.vkBits)
    }
}