package me.fexus.vulkan.component.pipeline

import me.fexus.vulkan.component.pipeline.shaderstage.ShaderStage

data class PushConstantsLayout(val size: Int, val offset: Int = 0, val shaderStages: ShaderStage = ShaderStage.BOTH)
