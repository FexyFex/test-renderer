package me.fexus.vulkan.pipeline

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.component.Device
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*


class GraphicsPipeline {
    var vkVertShaderModuleHandle: Long = 0L; private set
    var vkFragShaderModuleHandle: Long = 0L; private set
    var vkLayoutHandle: Long = 0L; private set
    var vkHandle: Long = 0L; private set


    fun create(device: Device, setLayout: Long, config: GraphicsPipelineConfiguration) = runMemorySafe {
        this@GraphicsPipeline.vkVertShaderModuleHandle = createShaderModule(device, config.vertShaderCode)
        this@GraphicsPipeline.vkFragShaderModuleHandle = createShaderModule(device, config.fragShaderCode)

        val pushConstantRange = calloc<VkPushConstantRange, VkPushConstantRange.Buffer>(1)
        pushConstantRange[0]
            .size(config.pushConstantsLayout.size)
            .offset(config.pushConstantsLayout.offset)
            .stageFlags(config.pushConstantsLayout.shaderStages.vkBits)

        val pSetLayout = allocateLong(1)
        pSetLayout.put(0, setLayout) // TODO: descriptor set layout

        val pipelineLayoutInfo = calloc<VkPipelineLayoutCreateInfo>() {
            sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
            pNext(0)
            flags(0)
            pSetLayouts(pSetLayout)
            pPushConstantRanges(pushConstantRange)
        }

        val pPipelineLayoutHandle = allocateLong(1)
        vkCreatePipelineLayout(device.vkHandle, pipelineLayoutInfo, null, pPipelineLayoutHandle)
        this@GraphicsPipeline.vkLayoutHandle = pPipelineLayoutHandle[0]

        val bindingDescription = calloc<VkVertexInputBindingDescription, VkVertexInputBindingDescription.Buffer>(1)
        bindingDescription[0]
            .binding(0)
            .stride(config.vertexStride)
            .inputRate(VK_VERTEX_INPUT_RATE_VERTEX)

        val attributeDescription = calloc<VkVertexInputAttributeDescription, VkVertexInputAttributeDescription.Buffer>(config.vertexAttributes.size)
        config.vertexAttributes.forEachIndexed { index, vertexAttribute ->
            attributeDescription[index]
                .binding(0)
                .location(vertexAttribute.location)
                .format(vertexAttribute.format.vkValue)
                .offset(vertexAttribute.offset)
        }

        val shaderStages = calloc<VkPipelineShaderStageCreateInfo, VkPipelineShaderStageCreateInfo.Buffer>(2)
        shaderStages[0]
            .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            .pNext(0)
            .stage(VK_SHADER_STAGE_VERTEX_BIT)
            .module(this@GraphicsPipeline.vkVertShaderModuleHandle)
            .pName(MemoryUtil.memUTF8("main"))
        shaderStages[1]
            .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            .pNext(0)
            .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
            .module(this@GraphicsPipeline.vkFragShaderModuleHandle)
            .pName(MemoryUtil.memUTF8("main"))

        val vertexInputInfo = calloc<VkPipelineVertexInputStateCreateInfo>() {
            sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
            pNext(0)
            pVertexBindingDescriptions(bindingDescription)
            pVertexAttributeDescriptions(attributeDescription)
        }

        val inputAssembly = calloc<VkPipelineInputAssemblyStateCreateInfo>() {
            sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
            pNext(0)
            topology(config.primitive.vkValue)
            primitiveRestartEnable(false)
        }

        // Dynamic states cover viewport and scissors so no there is need to give them any serious values
        val viewport = calloc<VkViewport, VkViewport.Buffer>(1)
        viewport[0]
            .x(0f).y(0f)
            .width(1f).height(1f)
            .minDepth(1f).maxDepth(0f)

        val scissors = calloc<VkRect2D, VkRect2D.Buffer>(1)
        scissors[0].offset().x(0).y(0)
        scissors[0].extent().width(1).height(1)

        val viewportState = calloc<VkPipelineViewportStateCreateInfo>() {
            sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
            pNext(0)
            viewportCount(1)
            pViewports(viewport)
            scissorCount(1)
            pScissors(scissors)
            flags(0)
        }

        val rasterizer = calloc<VkPipelineRasterizationStateCreateInfo>() {
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

        val multisampling = calloc<VkPipelineMultisampleStateCreateInfo>() {
            sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
            pNext(0)
            sampleShadingEnable(true)
            minSampleShading(0.2f)
            pSampleMask(null)
            alphaToCoverageEnable(false)
            alphaToOneEnable(false)
            rasterizationSamples(config.multisampling)
        }

        val colorBlendAttachment = calloc<VkPipelineColorBlendAttachmentState, VkPipelineColorBlendAttachmentState.Buffer>(1)
        colorBlendAttachment[0]
            .colorWriteMask(0xF) // RGBA
            .blendEnable(config.blendEnable)
            .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
            .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
            .colorBlendOp(VK_BLEND_OP_ADD)
            .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
            .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
            .alphaBlendOp(VK_BLEND_OP_ADD)

        val colorBlending = calloc<VkPipelineColorBlendStateCreateInfo>() {
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

        val depthStencil = calloc<VkPipelineDepthStencilStateCreateInfo>() {
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

        val pDynamicStates = allocateInt(2)
        pDynamicStates.put(0, VK_DYNAMIC_STATE_VIEWPORT)
        pDynamicStates.put(1, VK_DYNAMIC_STATE_SCISSOR)

        val dynamicState = calloc<VkPipelineDynamicStateCreateInfo>() {
            sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
            pNext(0)
            flags(0)
            pDynamicStates(pDynamicStates)
        }

        val pipelineInfo = calloc<VkGraphicsPipelineCreateInfo, VkGraphicsPipelineCreateInfo.Buffer>(1)
        pipelineInfo[0]
            .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
            .pNext(0)
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

        val pPipelineHandle = allocateLong(1)
        vkCreateGraphicsPipelines(device.vkHandle, 0, pipelineInfo, null, pPipelineHandle)
        this@GraphicsPipeline.vkHandle = pPipelineHandle[0]
    }

    private fun createShaderModule(device: Device, shaderCode: ByteArray): Long = runMemorySafe {
        val pCode = allocate(shaderCode.size)
        shaderCode.forEachIndexed { index, byte -> pCode.put(index, byte) }

        val moduleCreateInfo = calloc<VkShaderModuleCreateInfo>() {
            sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
            pNext(0)
            pCode(pCode)
            flags(0)
        }

        val pShaderModule = allocateLong(1)
        vkCreateShaderModule(device.vkHandle, moduleCreateInfo, null, pShaderModule)
        return@runMemorySafe pShaderModule[0]
    }

    fun destroy(device: Device) {
        vkDestroyShaderModule(device.vkHandle, vkVertShaderModuleHandle, null)
        vkDestroyShaderModule(device.vkHandle, vkFragShaderModuleHandle, null)
        vkDestroyPipelineLayout(device.vkHandle, vkLayoutHandle, null)
        vkDestroyPipeline(device.vkHandle, vkHandle, null)
    }
}