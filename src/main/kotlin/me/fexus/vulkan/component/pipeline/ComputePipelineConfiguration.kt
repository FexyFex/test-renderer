package me.fexus.vulkan.component.pipeline

import me.fexus.vulkan.component.pipeline.specializationconstant.SpecializationConstant


class ComputePipelineConfiguration (
    val shaderCode: ByteArray,
    val specializationConstants: List<SpecializationConstant<*>> = emptyList()
)