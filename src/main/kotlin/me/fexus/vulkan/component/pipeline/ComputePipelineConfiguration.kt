package me.fexus.vulkan.component.pipeline

import me.fexus.vulkan.component.pipeline.specializationconstant.SpecializationConstant


class ComputePipelineConfiguration (
    val shaderCode: ByteArray,
    val pushConstantsLayout: PushConstantsLayout? = null,
    val specializationConstants: List<SpecializationConstant<*>> = emptyList()
)