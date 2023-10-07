package me.fexus.vulkan.component.pipeline

data class PushConstantsLayout(val size: Int, val offset: Int = 0, val shaderStages: ShaderStage = ShaderStage.BOTH)
