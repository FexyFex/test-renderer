package me.fexus.fexgui

import me.fexus.examples.Globals
import me.fexus.fexgui.graphic.GlyphAtlas
import me.fexus.fexgui.graphic.GraphicalUIVulkan
import me.fexus.fexgui.graphic.vulkan.IndexedVulkanImage
import me.fexus.fexgui.logic.LogicalUI
import me.fexus.fexgui.logic.component.*
import me.fexus.memory.runMemorySafe
import me.fexus.model.QuadModel
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.accessmask.AccessMask
import me.fexus.vulkan.component.CommandBuffer
import me.fexus.vulkan.component.descriptor.pool.DescriptorPool
import me.fexus.vulkan.component.descriptor.pool.DescriptorPoolPlan
import me.fexus.vulkan.component.descriptor.pool.DescriptorPoolSize
import me.fexus.vulkan.component.descriptor.pool.flags.DescriptorPoolCreateFlag
import me.fexus.vulkan.component.descriptor.set.DescriptorSet
import me.fexus.vulkan.component.descriptor.set.layout.DescriptorSetLayout
import me.fexus.vulkan.component.descriptor.set.layout.DescriptorSetLayoutBinding
import me.fexus.vulkan.component.descriptor.set.layout.DescriptorSetLayoutPlan
import me.fexus.vulkan.component.descriptor.set.layout.bindingflags.DescriptorSetLayoutBindingFlag
import me.fexus.vulkan.component.descriptor.set.layout.createflags.DescriptorSetLayoutCreateFlag
import me.fexus.vulkan.component.descriptor.write.DescriptorBufferInfo
import me.fexus.vulkan.component.descriptor.write.DescriptorBufferWrite
import me.fexus.vulkan.component.descriptor.write.DescriptorImageInfo
import me.fexus.vulkan.component.descriptor.write.DescriptorImageWrite
import me.fexus.vulkan.component.pipeline.*
import me.fexus.vulkan.component.pipeline.pipelinestage.PipelineStage
import me.fexus.vulkan.component.pipeline.shaderstage.ShaderStage
import me.fexus.vulkan.descriptors.DescriptorType
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.image.*
import me.fexus.vulkan.descriptors.image.aspect.ImageAspect
import me.fexus.vulkan.descriptors.image.sampler.AddressMode
import me.fexus.vulkan.descriptors.image.sampler.Filtering
import me.fexus.vulkan.descriptors.image.sampler.VulkanSampler
import me.fexus.vulkan.descriptors.image.sampler.VulkanSamplerConfiguration
import me.fexus.vulkan.descriptors.image.usage.ImageUsage
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperty
import me.fexus.vulkan.util.ImageExtent3D
import me.fexus.window.Window
import me.fexus.window.input.InputHandler
import me.fexus.window.input.event.InputEventSubscriber
import org.lwjgl.vulkan.VK12
import java.nio.ByteBuffer
import java.nio.ByteOrder


class FexVulkanGUI private constructor(
    private val window: Window,
    private val inputHandler: InputHandler,
    private val deviceUtil: VulkanDeviceUtil,
    override val root: LogicalUIComponent,
): LogicalUI, GraphicalUIVulkan, LogicalUIComponent by root, InputEventSubscriber {
    private val glyphAtlas = GlyphAtlas()
    private lateinit var glyphAtlasImage: VulkanImage
    override lateinit var vertexBuffer: VulkanBuffer
    override lateinit var indexBuffer: VulkanBuffer
    private lateinit var sampler: VulkanSampler
    override val images = mutableListOf<IndexedVulkanImage>()
    private val screenSizeBuffers = mutableListOf<VulkanBuffer>()
    private val descriptorPool = DescriptorPool()
    private val descriptorSetLayout = DescriptorSetLayout()
    private val descriptorSets = Array(Globals.bufferStrategy) { DescriptorSet() }
    override val pipeline = GraphicsPipeline()
    private val assembledComponents = mutableListOf<AssembledComponent>()


    fun init() {
        subscribe(inputHandler)

        // Prepare GlyphAtlas image. Will always be in TRANSEFR_SRC mode because it's never sampled anyway
        val glyphAtlasImageConfig = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D,
            ImageExtent3D(glyphAtlas.texture.width, glyphAtlas.texture.height, 1),
            1, 1, 1, ImageColorFormat.R8G8B8A8_SRGB, ImageTiling.OPTIMAL,
            ImageAspect.COLOR, ImageUsage.SAMPLED + ImageUsage.TRANSFER_SRC + ImageUsage.TRANSFER_DST,
            MemoryProperty.DEVICE_LOCAL
        )
        this.glyphAtlasImage = deviceUtil.createImage(glyphAtlasImageConfig)

        val cmdBuf = deviceUtil.beginSingleTimeCommandBuffer()
        beginGUICommandBufferContext(cmdBuf) {
            transitionImageLayout(
                glyphAtlasImage,
                AccessMask.NONE, AccessMask.TRANSFER_READ,
                ImageLayout.UNDEFINED, ImageLayout.TRANSFER_SRC_OPTIMAL,
                PipelineStage.BOTTOM_OF_PIPE, PipelineStage.TRANSFER
            )
        }
        deviceUtil.endSingleTimeCommandBuffer(cmdBuf)

        createMeshBuffers()

        // -- DEFAULT SAMPLER --
        val samplerConfig = VulkanSamplerConfiguration(AddressMode.CLAMP_TO_EDGE, 1, Filtering.NEAREST)
        this.sampler = deviceUtil.createSampler(samplerConfig)
        // -- DEFAULT SAMPLER --

        // PIPELINE
        val pipelineConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC4, 0),
                VertexAttribute(1, VertexAttributeFormat.VEC4, 16),
            ),
            PushConstantsLayout(128),
            ClassLoader.getSystemResource("shaders/fexgui/standard_vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/fexgui/standard_frag.spv").readBytes(),
            dynamicStates = listOf(DynamicState.VIEWPORT, DynamicState.SCISSOR),
            blendEnable = true, cullMode = CullMode.NONE
        )
        this.pipeline.create(deviceUtil.device, descriptorSetLayout, pipelineConfig)
        // PIPELINE
    }

    private fun createMeshBuffers() {
        // VERETX BUFFER
        val vertexBufferSize = QuadModel.Vertex.SIZE_BYTES * QuadModel.vertices.size
        val vertexBufferConfig = VulkanBufferConfiguration(
            vertexBufferSize.toLong(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.VERTEX_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.vertexBuffer = deviceUtil.createBuffer(vertexBufferConfig)

        val vertexByteBuffer = ByteBuffer.allocate(vertexBufferSize)
        vertexByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        QuadModel.vertices.forEachIndexed { index, vertex ->
            val offset = index * QuadModel.Vertex.SIZE_BYTES
            vertex.writeToByteBuffer(vertexByteBuffer, offset)
        }
        deviceUtil.stagingCopy(vertexByteBuffer, vertexBuffer, 0L, 0L, vertexBufferSize.toLong())
        // VERETX BUFFER

        // INDEX BUFFER
        val indexBufferSize = QuadModel.indices.size * Int.SIZE_BYTES
        val indexBufferConfig = VulkanBufferConfiguration(
            indexBufferSize.toLong(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.INDEX_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.indexBuffer = deviceUtil.createBuffer(indexBufferConfig)

        val indexByteBuffer = ByteBuffer.allocate(indexBufferSize)
        indexByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        QuadModel.indices.forEachIndexed { index, cubeIndex ->
            val offset = index * Int.SIZE_BYTES
            indexByteBuffer.putInt(offset, cubeIndex)
        }

        deviceUtil.stagingCopy(indexByteBuffer, indexBuffer, 0L, 0L, indexBufferSize.toLong())
        // INDEX BUFFER
    }

    private fun writeScreenInfoBuffer(frameIndex: Int) {
        val extent = window.extent2D
        val buf = ByteBuffer.allocate(64)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(0, extent.x)
        buf.putInt(Int.SIZE_BYTES, extent.y)

        screenSizeBuffers[frameIndex].put(0, buf)
    }

    private fun createDescriptorSet() {
        // Descriptor Sets
        val poolPlan = DescriptorPoolPlan(
            Globals.bufferStrategy, DescriptorPoolCreateFlag.FREE_DESCRIPTOR_SET,
            listOf(
                DescriptorPoolSize(DescriptorType.UNIFORM_BUFFER, 4),
                DescriptorPoolSize(DescriptorType.SAMPLED_IMAGE, 16),
                DescriptorPoolSize(DescriptorType.SAMPLER, 1)
            )
        )
        this.descriptorPool.create(deviceUtil.device, poolPlan)

        val setLayoutPlan = DescriptorSetLayoutPlan(
            DescriptorSetLayoutCreateFlag.NONE,
            listOf(
                DescriptorSetLayoutBinding(
                    0, 1,
                    DescriptorType.UNIFORM_BUFFER,
                    ShaderStage.VERTEX,
                    DescriptorSetLayoutBindingFlag.NONE
                ),
                DescriptorSetLayoutBinding(
                    1, 16,
                    DescriptorType.SAMPLED_IMAGE,
                    ShaderStage.FRAGMENT,
                    DescriptorSetLayoutBindingFlag.PARTIALLY_BOUND
                ),
                DescriptorSetLayoutBinding(
                    2, 1,
                    DescriptorType.SAMPLER,
                    ShaderStage.FRAGMENT,
                    DescriptorSetLayoutBindingFlag.NONE
                )
            )
        )
        this.descriptorSetLayout.create(deviceUtil.device, setLayoutPlan)

        this.descriptorSets.forEachIndexed { index, descriptorSet ->
            descriptorSet.create(deviceUtil.device, descriptorPool, descriptorSetLayout)

            // Update Descriptor Set
            val descWriteCameraBuf = DescriptorBufferWrite(
                0, DescriptorType.UNIFORM_BUFFER, 1, descriptorSet, 0,
                listOf(DescriptorBufferInfo(screenSizeBuffers[index].vkBufferHandle, 0L, VK12.VK_WHOLE_SIZE))
            )
            val descWriteTextures = DescriptorImageWrite(
                1, DescriptorType.SAMPLED_IMAGE, images.size, descriptorSet, 0,
                images.map { DescriptorImageInfo(0L, it.image.vkImageViewHandle, ImageLayout.SHADER_READ_ONLY_OPTIMAL) }
            )
            val descWriteSampler = DescriptorImageWrite(
                2, DescriptorType.SAMPLER, 1, descriptorSet, 0,
                listOf(DescriptorImageInfo(sampler.vkHandle, 0L , ImageLayout.SHADER_READ_ONLY_OPTIMAL))
            )

            descriptorSet.update(deviceUtil.device, descWriteCameraBuf, descWriteTextures, descWriteSampler)
        }
    }


    fun tick() {

    }


    fun recordGUIRenderCommands(
        cmdBuf: CommandBuffer,
        frameIndex: Int,
        frameInFlightIndex: Int
    ) = beginGUICommandBufferContext(cmdBuf) {
        writeScreenInfoBuffer(frameIndex)

        assembledComponents
            .filter { it.logicComponent is TextComponent && it.logicComponent.textRequiresUpdate }
            .forEach { updateTextComponent(it) }

        bindPipeline()
        bindVertexBuffer(vertexBuffer)
        bindIndexBuffer(indexBuffer)

        runMemorySafe {
            val pushConsts = allocate(128)
            assembledComponents.forEach {
                it.writePushConstantsBuffer(pushConsts)
                pushConstants(pushConsts)
                drawIndexed()
            }
        }
    }

    private fun updateTextComponent(component: AssembledComponent) {

    }


    fun assembleComponent(component: LogicalUIComponent) {

    }


    fun disassembleDeadComponents() {

    }


    companion object {
        fun create(window: Window, inputHandler: InputHandler, deviceUtil: VulkanDeviceUtil) =
            FexVulkanGUI(window, inputHandler, deviceUtil, PhantomComponent(null))
    }
}