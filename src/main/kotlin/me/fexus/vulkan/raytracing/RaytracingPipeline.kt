package me.fexus.vulkan.raytracing

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.component.Device
import me.fexus.vulkan.component.descriptor.set.layout.DescriptorSetLayout
import me.fexus.vulkan.component.pipeline.IPipeline
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRRayTracingPipeline.*
import org.lwjgl.vulkan.VK12.*


class RaytracingPipeline: IPipeline {
    override var vkHandle: Long = 0L; private set
    override var vkLayoutHandle: Long = 0L; private set
    private val shaderModules = mutableListOf<Long>()

    var shadergroupCount: Int = 0; private set


    fun create(device: Device, setLayout: DescriptorSetLayout, config: RaytracingPipelineConfiguration) = runMemorySafe {
        val pushConstantRange = calloc(VkPushConstantRange::calloc, 1)
        pushConstantRange[0]
            .stageFlags(config.pushConstantsLayout.shaderStages.vkBits)
            .offset(config.pushConstantsLayout.offset)
            .size(config.pushConstantsLayout.size)

        val pipelineLayoutInfo = calloc(VkPipelineLayoutCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
            pNext(0L)
            flags(0)
            pPushConstantRanges(pushConstantRange)
            pSetLayouts(allocateLongValues(setLayout.vkHandle))
            setLayoutCount(1)
        }

        val pLayoutHandle = allocateLong(1)
        vkCreatePipelineLayout(device.vkHandle, pipelineLayoutInfo, null, pLayoutHandle)
        this@RaytracingPipeline.vkLayoutHandle = pLayoutHandle[0]

        val stages = calloc(VkPipelineShaderStageCreateInfo::calloc, config.shaderStages.size)

        config.shaderStages.forEachIndexed { index, stage ->
            val shaderModule = createShaderModule(device, stage.shaderCode)
            shaderModules.add(shaderModule)

            stages[index]
                .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .pNext(0L)
                .flags(0)
                .pName(allocateStringValue("main"))
                .module(shaderModule)
                .stage(stage.stageType.vkBits)
                .pSpecializationInfo(null)
        }

        val groups = calloc(VkRayTracingShaderGroupCreateInfoKHR::calloc, config.shaderGroups.size)

        config.shaderGroups.forEachIndexed { index, group ->
            groups[index]
                .sType(VK_STRUCTURE_TYPE_RAY_TRACING_SHADER_GROUP_CREATE_INFO_KHR)
                .pNext(0L)
                .type(group.type.vkValue)
                .generalShader(group.generalShaderIndex)
                .closestHitShader(group.closestHitShaderIndex)
                .anyHitShader(group.anyHitShaderIndex)
                .intersectionShader(group.intersectionShaderIndex)
        }

        this@RaytracingPipeline.shadergroupCount = config.shaderGroups.size

        val raytracingPipelineCreateInfo = calloc(VkRayTracingPipelineCreateInfoKHR::calloc, 1)
        raytracingPipelineCreateInfo[0]
            .sType(VK_STRUCTURE_TYPE_RAY_TRACING_PIPELINE_CREATE_INFO_KHR)
            .pNext(0L)
            .pStages(stages)
            .pGroups(groups)
            .maxPipelineRayRecursionDepth(2)
            .pLibraryInfo(null)
            .pLibraryInterface(null)
            .pDynamicState(null)
            .layout(this@RaytracingPipeline.vkLayoutHandle)
            .basePipelineHandle(0L)
            .basePipelineIndex(0)

        if (config.dynamicStates.isNotEmpty()) {
            val dynamicStates = calloc(VkPipelineDynamicStateCreateInfo::calloc) {
                sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                pNext(0L)
                flags(0)
                pDynamicStates(allocateIntValues(*config.dynamicStates.map { it.vkValue }.toIntArray()))
            }
            raytracingPipelineCreateInfo.pDynamicState(dynamicStates)
        }

        val pPipelineHandle = allocateLong(1)
        vkCreateRayTracingPipelinesKHR(
            device.vkHandle, 0L, 0L,
            raytracingPipelineCreateInfo, null, pPipelineHandle
        )
        this@RaytracingPipeline.vkHandle = pPipelineHandle[0]

        return@runMemorySafe this@RaytracingPipeline
    }


    fun destroy(device: Device) {
        shaderModules.forEach { vkDestroyShaderModule(device.vkHandle, it, null) }
        shaderModules.clear()
        vkDestroyPipelineLayout(device.vkHandle, vkLayoutHandle, null)
        vkDestroyPipeline(device.vkHandle, vkHandle, null)
    }
}