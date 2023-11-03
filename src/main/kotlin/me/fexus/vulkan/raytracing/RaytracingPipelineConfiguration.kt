package me.fexus.vulkan.raytracing

import me.fexus.vulkan.component.pipeline.DynamicState
import me.fexus.vulkan.component.pipeline.PushConstantsLayout

data class RaytracingPipelineConfiguration(
    val shaderStages: List<RaytracingShaderStage>,
    val shaderGroups: List<RaytracingShaderGroup>,
    val pushConstantsLayout: PushConstantsLayout,
    val dynamicStates: List<DynamicState> = emptyList(),
)