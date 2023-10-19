package me.fexus.vulkan.raytracing

import me.fexus.vulkan.component.pipeline.shaderstage.IShaderStage


data class RaytracingShaderStage(val stageType: IShaderStage, val groupType: RaytracingShaderGroupType, val shaderCode: ByteArray)
