package me.fexus.vulkan.component.pipeline

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.component.Device
import me.fexus.vulkan.component.descriptor.set.layout.DescriptorSetLayout
import me.fexus.vulkan.component.pipeline.specializationconstant.SpecializationConstantFloat
import me.fexus.vulkan.component.pipeline.specializationconstant.SpecializationConstantInt
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*


class ComputePipeline : IPipeline {
    private var vkShaderModuleHandle: Long = 0L
    override var vkHandle: Long = 0L; private set
    override var vkLayoutHandle: Long = 0L; private set

    fun create(device: Device, setLayout: DescriptorSetLayout, config: ComputePipelineConfiguration) = runMemorySafe {
        val pushConstantRange = calloc(VkPushConstantRange::calloc, 1)
        pushConstantRange[0].set(VK_SHADER_STAGE_COMPUTE_BIT, 0, 128)

        val pSetLayouts = allocateLong(1)
        pSetLayouts.put(0, setLayout.vkHandle)
        val layoutCreateInfo = calloc(VkPipelineLayoutCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
            pNext(0)
            flags(0)
            setLayoutCount(1)
            pSetLayouts(pSetLayouts)
            pPushConstantRanges(pushConstantRange)
        }

        val pPipelineLayoutHandle = allocateLong(1)
        vkCreatePipelineLayout(device.vkHandle, layoutCreateInfo, null, pPipelineLayoutHandle)
        this@ComputePipeline.vkLayoutHandle = pPipelineLayoutHandle[0]

        val shaderModule = createShaderModule(device, config.shaderCode)
        this@ComputePipeline.vkShaderModuleHandle = shaderModule

        val specMap = calloc(VkSpecializationMapEntry::calloc, config.specializationConstants.size)
        val pSpecData = allocate(config.specializationConstants.size * 4)
        config.specializationConstants.forEachIndexed { index, specConst ->
            specMap[index].constantID(specConst.id).size(4L).offset(index * 4)
            when (specConst) {
                is SpecializationConstantInt -> pSpecData.putInt(index * 4, specConst.value)
                is SpecializationConstantFloat -> pSpecData.putFloat(index * 4, specConst.value)
                else -> throw Exception("meh")
            }
        }
        val specInfo = calloc(VkSpecializationInfo::calloc) {
            pMapEntries(specMap)
            pData(pSpecData)
        }

        val pEntryPoint = allocateString("main")
        val shaderStageInfo = calloc(VkPipelineShaderStageCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            pNext(0)
            stage(VK_SHADER_STAGE_COMPUTE_BIT)
            module(shaderModule)
            pName(pEntryPoint)
            pSpecializationInfo(specInfo)
        }

        val createInfo = calloc(VkComputePipelineCreateInfo::calloc, 1)
        createInfo[0]
            .sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
            .pNext(0)
            .layout(this@ComputePipeline.vkLayoutHandle)
            .flags(0)
            .stage(shaderStageInfo)

        val pPipelineHandle = allocateLong(1)
        vkCreateComputePipelines(device.vkHandle, 0, createInfo, null, pPipelineHandle)
        this@ComputePipeline.vkHandle = pPipelineHandle[0]

        return@runMemorySafe this@ComputePipeline
    }


    fun destroy(device: Device) {
        vkDestroyShaderModule(device.vkHandle, vkShaderModuleHandle, null)
        vkDestroyPipelineLayout(device.vkHandle, vkLayoutHandle, null)
        vkDestroyPipeline(device.vkHandle, vkHandle, null)
    }
}