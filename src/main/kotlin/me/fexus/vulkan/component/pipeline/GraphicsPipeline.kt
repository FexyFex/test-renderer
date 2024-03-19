package me.fexus.vulkan.component.pipeline

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.component.Device
import me.fexus.vulkan.component.descriptor.set.layout.DescriptorSetLayout
import me.fexus.vulkan.component.pipeline.specializationconstant.SpecializationConstantFloat
import me.fexus.vulkan.component.pipeline.specializationconstant.SpecializationConstantInt
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRDynamicRendering.VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO_KHR
import org.lwjgl.vulkan.VK12.*


class GraphicsPipeline: IPipeline {
    private var vkVertShaderModuleHandle: Long = 0L
    private var vkFragShaderModuleHandle: Long = 0L
    override var vkLayoutHandle: Long = 0L; private set
    override var vkHandle: Long = 0L; private set


    fun create(device: Device, setLayouts: List<DescriptorSetLayout>, config: GraphicsPipelineConfiguration) = runMemorySafe {
        this@GraphicsPipeline.vkVertShaderModuleHandle = createShaderModule(device, config.vertShaderCode)
        this@GraphicsPipeline.vkFragShaderModuleHandle = createShaderModule(device, config.fragShaderCode)

        val pushConstantRange = calloc(VkPushConstantRange::calloc, 1)
        pushConstantRange[0]
            .size(config.pushConstantsLayout.size)
            .offset(config.pushConstantsLayout.offset)
            .stageFlags(config.pushConstantsLayout.shaderStages.vkBits)

        val pSetLayout = allocateLong(setLayouts.size)
        setLayouts.forEachIndexed { index, descSetLayout ->
            pSetLayout.put(index, descSetLayout.vkHandle)
        }

        val pipelineLayoutInfo = calloc(VkPipelineLayoutCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
            pNext(0)
            flags(0)
            pSetLayouts(pSetLayout)
            pPushConstantRanges(pushConstantRange)
        }

        val pPipelineLayoutHandle = allocateLong(1)
        vkCreatePipelineLayout(device.vkHandle, pipelineLayoutInfo, null, pPipelineLayoutHandle)
        this@GraphicsPipeline.vkLayoutHandle = pPipelineLayoutHandle[0]

        val vertexBindingDescription = calloc(VkVertexInputBindingDescription::calloc, config.vertexInputBindings.size)
        config.vertexInputBindings.forEachIndexed { index, vertexInputBinding ->
            vertexBindingDescription[index]
                .binding(vertexInputBinding.binding)
                .stride(vertexInputBinding.stride)
                .inputRate(vertexInputBinding.inputRate.vkEnum)
        }

        val vertexAttributeDescription = calloc(VkVertexInputAttributeDescription::calloc, config.vertexAttributes.size)
        config.vertexAttributes.forEachIndexed { index, vertexAttribute ->
            vertexAttributeDescription[index]
                .binding(0)
                .location(vertexAttribute.location)
                .format(vertexAttribute.format.vkValue)
                .offset(vertexAttribute.offset)
        }

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

        val shaderStages = calloc(VkPipelineShaderStageCreateInfo::calloc, 2)
        shaderStages[0]
            .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            .pNext(0)
            .stage(VK_SHADER_STAGE_VERTEX_BIT)
            .module(this@GraphicsPipeline.vkVertShaderModuleHandle)
            .pName(MemoryUtil.memUTF8("main"))
            .pSpecializationInfo(specInfo)
        shaderStages[1]
            .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            .pNext(0)
            .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
            .module(this@GraphicsPipeline.vkFragShaderModuleHandle)
            .pName(MemoryUtil.memUTF8("main"))
            .pSpecializationInfo(specInfo)

        val vertexInputInfo = calloc(VkPipelineVertexInputStateCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
            pNext(0)
            pVertexBindingDescriptions(vertexBindingDescription)
            pVertexAttributeDescriptions(vertexAttributeDescription)
        }

        val inputAssembly = calloc(VkPipelineInputAssemblyStateCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
            pNext(0)
            topology(config.primitive.vkValue)
            primitiveRestartEnable(false)
        }

        // Dynamic states cover viewport and scissors so no there is need to give them any serious values
        val viewport = calloc(VkViewport::calloc, 1)
        viewport[0]
            .x(0f).y(0f)
            .width(1f).height(1f)
            .minDepth(1f).maxDepth(0f)

        val scissors = calloc(VkRect2D::calloc, 1)
        scissors[0].offset().x(0).y(0)
        scissors[0].extent().width(1).height(1)

        val viewportState = calloc(VkPipelineViewportStateCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
            pNext(0)
            viewportCount(1)
            pViewports(viewport)
            scissorCount(1)
            pScissors(scissors)
            flags(0)
        }

        val rasterizer = calloc(VkPipelineRasterizationStateCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
            pNext(0)
            depthClampEnable(false)
            rasterizerDiscardEnable(false)
            polygonMode(config.polygonMode.vkValue)
            lineWidth(1.0f)
            cullMode(config.cullMode.vkValue)
            frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
            depthBiasEnable(false)
            depthBiasConstantFactor(0.0f)
            depthBiasClamp(0.0f)
            depthBiasSlopeFactor(0.0f)
        }

        val multisampling = calloc(VkPipelineMultisampleStateCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
            pNext(0)
            sampleShadingEnable(true)
            minSampleShading(0.2f)
            pSampleMask(null)
            alphaToCoverageEnable(false)
            alphaToOneEnable(false)
            rasterizationSamples(config.multisampling)
        }

        val colorBlendAttachment = calloc(VkPipelineColorBlendAttachmentState::calloc, 1)
        colorBlendAttachment[0]
            .colorWriteMask(0xF) // RGBA
            .blendEnable(config.blendEnable)
            .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
            .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
            .colorBlendOp(VK_BLEND_OP_ADD)
            .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
            .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
            .alphaBlendOp(VK_BLEND_OP_ADD)

        val colorBlending = calloc(VkPipelineColorBlendStateCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
            pNext(0)
            logicOpEnable(false)
            logicOp(VK_LOGIC_OP_COPY)
            pAttachments(colorBlendAttachment)
            blendConstants(0, 0f)
            blendConstants(1, 0f)
            blendConstants(2, 0f)
            blendConstants(3, 0f)
        }

        val depthStencil = calloc(VkPipelineDepthStencilStateCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
            pNext(0)
            depthTestEnable(config.depthTest)
            depthWriteEnable(config.depthWrite)
            depthCompareOp(VK_COMPARE_OP_GREATER_OR_EQUAL)
            depthBoundsTestEnable(false)
            minDepthBounds(1.0f)
            maxDepthBounds(0.0f)
            stencilTestEnable(false)
        }

        val pDynamicStates = allocateInt(config.dynamicStates.size)
        config.dynamicStates.forEachIndexed { index, dynamicState ->
            pDynamicStates.put(index, dynamicState.vkValue)
        }

        val dynamicState = calloc(VkPipelineDynamicStateCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
            pNext(0)
            flags(0)
            pDynamicStates(pDynamicStates)
        }

        val pColorAttachmentFormats = allocateInt(1)
        pColorAttachmentFormats.put(0, VK_FORMAT_B8G8R8A8_SRGB)

        val renderingInfo = calloc(VkPipelineRenderingCreateInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO_KHR)
            pNext(0)
            viewMask(0)
            colorAttachmentCount(1)
            pColorAttachmentFormats(pColorAttachmentFormats)
            depthAttachmentFormat(VK_FORMAT_D32_SFLOAT)
            stencilAttachmentFormat(VK_FORMAT_UNDEFINED)
        }

        val pipelineInfo = calloc(VkGraphicsPipelineCreateInfo::calloc, 1)
        pipelineInfo[0]
            .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
            .pNext(renderingInfo.address())
            .pStages(shaderStages)
            .pVertexInputState(vertexInputInfo)
            .pInputAssemblyState(inputAssembly)
            .pViewportState(viewportState)
            .pRasterizationState(rasterizer)
            .pMultisampleState(multisampling)
            .pDepthStencilState(null)
            .pColorBlendState(colorBlending)
            .layout(this@GraphicsPipeline.vkLayoutHandle)
            .subpass(0)
            .basePipelineHandle(VK_NULL_HANDLE)
            .basePipelineIndex(-1)
            .pDepthStencilState(depthStencil)
            .pDynamicState(dynamicState)
            .renderPass(0) // No render pass. Dynamic rendering is used

        val pPipelineHandle = allocateLong(1)
        vkCreateGraphicsPipelines(device.vkHandle, 0, pipelineInfo, null, pPipelineHandle)
        this@GraphicsPipeline.vkHandle = pPipelineHandle[0]
    }

    fun destroy(device: Device) {
        vkDestroyShaderModule(device.vkHandle, vkVertShaderModuleHandle, null)
        vkDestroyShaderModule(device.vkHandle, vkFragShaderModuleHandle, null)
        vkDestroyPipelineLayout(device.vkHandle, vkLayoutHandle, null)
        vkDestroyPipeline(device.vkHandle, vkHandle, null)
    }
}