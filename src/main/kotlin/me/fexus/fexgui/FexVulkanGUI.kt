package me.fexus.fexgui

import me.fexus.examples.Globals
import me.fexus.fexgui.graphic.ComponentRenderer
import me.fexus.fexgui.graphic.GlyphAtlas
import me.fexus.fexgui.graphic.GraphicalUIVulkan
import me.fexus.fexgui.graphic.vulkan.IndexedVulkanImage
import me.fexus.fexgui.graphic.vulkan.util.ImageBlit
import me.fexus.fexgui.graphic.vulkan.util.ImageRegion
import me.fexus.fexgui.logic.LogicalUI
import me.fexus.fexgui.logic.component.*
import me.fexus.fexgui.logic.component.visual.flag.VisualFlag
import me.fexus.fexgui.textureresource.GUIFilledTextureResource
import me.fexus.fexgui.textureresource.GUITextureResource
import me.fexus.math.vec.IVec2
import me.fexus.math.vec.Vec4
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
import me.fexus.vulkan.component.descriptor.write.*
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
import me.fexus.vulkan.util.Offset3D
import me.fexus.window.Window
import me.fexus.window.input.InputHandler
import me.fexus.window.input.event.InputEventSubscriber
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK12.VK_WHOLE_SIZE
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min


class FexVulkanGUI (
    private val window: Window,
    private val inputHandler: InputHandler,
    private val deviceUtil: VulkanDeviceUtil,
): LogicalUI, GraphicalUIVulkan, LogicalUIComponent, InputEventSubscriber {
    // LogicalUI overrides
    override val root: LogicalUIComponent = PhantomComponent(null)
    // LogicalUIComponent overrides
    override val parent: LogicalUIComponent? = null
    override val children: MutableList<LogicalUIComponent>; get() = root.children
    override var destroyed: Boolean; get() = root.destroyed; set(value) { root.destroyed = value }

    // Graphical stuff
    private val device = deviceUtil.device
    private val glyphAtlas = GlyphAtlas()
    private lateinit var glyphAtlasImage: VulkanImage
    override lateinit var vertexBuffer: VulkanBuffer
    override lateinit var indexBuffer: VulkanBuffer
    private lateinit var sampler: VulkanSampler
    private val imageResources = mutableMapOf<GUITextureResource, IndexedVulkanImage>()
    private val imageIndices = mutableMapOf<Int, IndexedVulkanImage>()
    private val images = mutableListOf<IndexedVulkanImage>()
    private lateinit var screenSizeBuffers: List<VulkanBuffer>
    override val pipeline = GraphicsPipeline()
    private val componentRenderers = mutableListOf<ComponentRenderer>()
    // Descriptor Set stuff
    private val descriptorPool = DescriptorPool()
    private val descriptorSetLayout = DescriptorSetLayout()
    private val descriptorSets = Array(Globals.framesTotal) { DescriptorSet() }
    private val imagesDescriptorSetLayout = DescriptorSetLayout()
    private val imagesDescriptorSet = DescriptorSet() // Images get their own descriptor set (UPDATE_AFTER_BIND)


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

        val glyphAtlasImageStagingBufferConfig = VulkanBufferConfiguration(
            glyphAtlas.texture.imageSize,
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.TRANSFER_SRC
        )
        val glyphsStagingBuffer = deviceUtil.createBuffer(glyphAtlasImageStagingBufferConfig)
        glyphsStagingBuffer.copy(MemoryUtil.memAddress(glyphAtlas.texture.pixels), 0L, glyphAtlas.texture.imageSize)

        val cmdBuf = deviceUtil.beginSingleTimeCommandBuffer()
        beginGUICommandRecordContext(cmdBuf) {
            transitionImageLayout(
                glyphAtlasImage,
                AccessMask.NONE, AccessMask.TRANSFER_WRITE,
                ImageLayout.UNDEFINED, ImageLayout.TRANSFER_DST_OPTIMAL,
                PipelineStage.BOTTOM_OF_PIPE, PipelineStage.TRANSFER
            )

            copyBufferToImage(glyphsStagingBuffer, glyphAtlasImage)

            transitionImageLayout(
                glyphAtlasImage,
                AccessMask.TRANSFER_WRITE, AccessMask.TRANSFER_READ,
                ImageLayout.TRANSFER_DST_OPTIMAL, ImageLayout.TRANSFER_SRC_OPTIMAL,
                PipelineStage.TRANSFER, PipelineStage.TRANSFER
            )
        }
        deviceUtil.endSingleTimeCommandBuffer(cmdBuf)
        glyphsStagingBuffer.destroy()

        // -- SCREEN SIZE BUFFERS --
        val buffers = mutableListOf<VulkanBuffer>()
        val screenSizeBufferConfig = VulkanBufferConfiguration(
            64L,
            MemoryProperty.HOST_COHERENT + MemoryProperty.HOST_VISIBLE,
            BufferUsage.UNIFORM_BUFFER
        )
        repeat(Globals.framesTotal) {
            val buf = deviceUtil.createBuffer(screenSizeBufferConfig)
            buffers.add(buf)
        }
        this.screenSizeBuffers = buffers
        // -- SCREEN SIZE BUFFERS --

        // -- DEFAULT SAMPLER --
        val samplerConfig = VulkanSamplerConfiguration(AddressMode.CLAMP_TO_EDGE, 1, Filtering.NEAREST)
        this.sampler = deviceUtil.createSampler(samplerConfig)
        // -- DEFAULT SAMPLER --

        createMeshBuffers()

        createDescriptorSets()

        // PIPELINE
        val pipelineConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC4, 0),
                VertexAttribute(1, VertexAttributeFormat.VEC4, 16),
            ),
            PushConstantsLayout(128),
            ClassLoader.getSystemResource("fexgui/shaders/standard_vert.spv").readBytes(),
            ClassLoader.getSystemResource("fexgui/shaders/standard_frag.spv").readBytes(),
            dynamicStates = listOf(DynamicState.VIEWPORT, DynamicState.SCISSOR),
            blendEnable = true, cullMode = CullMode.NONE
        )
        this.pipeline.create(device, listOf(descriptorSetLayout, imagesDescriptorSetLayout), pipelineConfig)
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

    private fun createDescriptorSets() {
        // Descriptor Sets
        val poolPlan = DescriptorPoolPlan(
            16,
            DescriptorPoolCreateFlag.FREE_DESCRIPTOR_SET + DescriptorPoolCreateFlag.UPDATE_AFTER_BIND,
            listOf(
                DescriptorPoolSize(DescriptorType.UNIFORM_BUFFER, 16),
                DescriptorPoolSize(DescriptorType.SAMPLED_IMAGE, MAX_IMAGE_COUNT),
                DescriptorPoolSize(DescriptorType.SAMPLER, 16)
            )
        )
        this.descriptorPool.create(device, poolPlan)

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
                    1, 1,
                    DescriptorType.SAMPLER,
                    ShaderStage.FRAGMENT,
                    DescriptorSetLayoutBindingFlag.NONE
                )
            )
        )
        this.descriptorSetLayout.create(device, setLayoutPlan)

        this.descriptorSets.forEachIndexed { index, descriptorSet ->
            descriptorSet.create(device, descriptorPool, descriptorSetLayout)

            // Update Descriptor Set
            val descWriteCameraBuf = DescriptorBufferWrite(
                0, DescriptorType.UNIFORM_BUFFER, 1, descriptorSet, 0,
                listOf(DescriptorBufferInfo(screenSizeBuffers[index].vkBufferHandle, 0L, VK_WHOLE_SIZE))
            )
            val descWriteSampler = DescriptorImageWrite(
                1, DescriptorType.SAMPLER, 1, descriptorSet, 0,
                listOf(DescriptorImageInfo(sampler.vkHandle, 0L , ImageLayout.SHADER_READ_ONLY_OPTIMAL))
            )

            descriptorSet.update(device, descWriteCameraBuf, descWriteSampler)
        }

        val imagesSetLayoutPlan = DescriptorSetLayoutPlan(
            DescriptorSetLayoutCreateFlag.UPDATE_AFTER_BIND,
            listOf(
                DescriptorSetLayoutBinding(
                    0, MAX_IMAGE_COUNT,
                    DescriptorType.SAMPLED_IMAGE,
                    ShaderStage.FRAGMENT,
                    DescriptorSetLayoutBindingFlag.PARTIALLY_BOUND + DescriptorSetLayoutBindingFlag.UPDATE_AFTER_BIND
                ),
            )
        )
        this.imagesDescriptorSetLayout.create(device, imagesSetLayoutPlan)

        this.imagesDescriptorSet.create(device, descriptorPool, imagesDescriptorSetLayout)
        updateImageDescriptorSet()
    }

    private fun updateImageDescriptorSet() {
        val writes = images.map {
            DescriptorImageWrite(
                0, DescriptorType.SAMPLED_IMAGE, 1, imagesDescriptorSet, it.index,
                listOf(DescriptorImageInfo(0L, it.image.vkImageViewHandle, ImageLayout.SHADER_READ_ONLY_OPTIMAL))
            )
        }
        imagesDescriptorSet.update(device, writes)
    }


    override fun signalComponentAdded(component: SpatialComponent) = assembleComponent(component)

    private fun assembleComponent(component: SpatialComponent) {
        val images: List<IndexedVulkanImage?> = component.visualLayout.subComponents.map {
            return@map when {
                VisualFlag.TEXT_IMAGE in it.visualFlags -> {
                    val res = it.textureResource!!
                    val image = createImage(res.width, res.height)
                    val imageIndex = findNextImageIndex()
                    val lIndexedImage = IndexedVulkanImage(imageIndex, image)
                    images.add(lIndexedImage)
                    imageResources[res] = lIndexedImage
                    imageIndices[imageIndex] = lIndexedImage
                    updateImageDescriptorSet()
                    lIndexedImage
                }
                VisualFlag.TEXTURED in it.visualFlags -> {
                    val res = it.textureResource as GUIFilledTextureResource
                    val existingImage = imageResources[res]
                    if (existingImage == null) {
                        val image = createImage(res.width, res.height)
                        val imageIndex = findNextImageIndex()
                        setImageTexture(image, res.pixelBuffer)
                        val lIndexedImage = IndexedVulkanImage(imageIndex, image)
                        images.add(lIndexedImage)
                        imageResources[res] = lIndexedImage
                        imageIndices[imageIndex] = lIndexedImage
                        updateImageDescriptorSet()
                        lIndexedImage
                    } else {
                        existingImage
                    }
                }
                else -> null
            }
        }

        val renderer = ComponentRenderer(component, images)
        componentRenderers.add(renderer)
    }

    private fun findNextImageIndex(): Int {
        repeat(MAX_IMAGE_COUNT) {
            if (imageIndices[it] == null) return it
        }
        throw Exception("No more GUI images available")
    }

    private fun createImage(width: Int, height: Int): VulkanImage {
        val imageConfig = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D, ImageExtent3D(width, height, 1),
            1, 1, 1, ImageColorFormat.R8G8B8A8_SRGB, ImageTiling.OPTIMAL,
            ImageAspect.COLOR, ImageUsage.SAMPLED + ImageUsage.TRANSFER_DST, MemoryProperty.DEVICE_LOCAL
        )
        val newImage = deviceUtil.createImage(imageConfig)

        val cmdBuf = deviceUtil.beginSingleTimeCommandBuffer()
        beginGUICommandRecordContext(cmdBuf) {
            transitionImageLayout(
                newImage,
                AccessMask.NONE, AccessMask.SHADER_READ,
                ImageLayout.UNDEFINED, ImageLayout.SHADER_READ_ONLY_OPTIMAL,
                PipelineStage.BOTTOM_OF_PIPE, PipelineStage.FRAGMENT_SHADER
            )
        }
        deviceUtil.endSingleTimeCommandBuffer(cmdBuf)

        return newImage
    }

    private fun setImageTexture(image: VulkanImage, pixelBuffer: ByteBuffer) {
        val bufferSize = pixelBuffer.capacity().toLong()
        val stagingBufConfig = VulkanBufferConfiguration(
            bufferSize,
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.TRANSFER_SRC
        )
        val stagingBuffer = deviceUtil.createBuffer(stagingBufConfig)

        stagingBuffer.copy(MemoryUtil.memAddress(pixelBuffer), 0L, bufferSize)

        val cmdBuf = deviceUtil.beginSingleTimeCommandBuffer()
        beginGUICommandRecordContext(cmdBuf) {
            transitionImageLayout(
                image,
                AccessMask.SHADER_READ, AccessMask.TRANSFER_WRITE,
                ImageLayout.SHADER_READ_ONLY_OPTIMAL, ImageLayout.TRANSFER_DST_OPTIMAL,
                PipelineStage.FRAGMENT_SHADER, PipelineStage.TRANSFER
            )

            copyBufferToImage(stagingBuffer, image)

            transitionImageLayout(
                image,
                AccessMask.TRANSFER_WRITE, AccessMask.SHADER_READ,
                ImageLayout.TRANSFER_DST_OPTIMAL, ImageLayout.SHADER_READ_ONLY_OPTIMAL,
                PipelineStage.TRANSFER, PipelineStage.FRAGMENT_SHADER
            )
        }
        deviceUtil.endSingleTimeCommandBuffer(cmdBuf)

        stagingBuffer.destroy()
    }


    // Must be called outside any RenderPass
    fun recordOffRenderPassCommands(cmdBuf: CommandBuffer, frameIndex: Int) = beginGUICommandRecordContext(cmdBuf) {
        componentRenderers
            .filter { it.logicComponent is TextComponent && it.logicComponent.textRequiresUpdate }
            .forEach { updateTextComponent(this, it) }
    }

    // Must be called within any RenderPass
    fun recordRenderPassCommands(cmdBuf: CommandBuffer, frameIndex: Int) = beginGUICommandRecordContext(cmdBuf) {
        writeScreenInfoBuffer(frameIndex)

        bindPipeline()
        bindDescriptorSets(descriptorSets[frameIndex], imagesDescriptorSet)
        bindVertexBuffer(vertexBuffer)
        bindIndexBuffer(indexBuffer)

        componentRenderers.forEach { it.render(this) }
    }

    private fun updateTextComponent(cmdContext: GraphicalUIVulkan.CommandBufferContext, component: ComponentRenderer) {
        component.logicComponent as TextComponent
        if (component.logicComponent.textRequiresUpdate) {
            blitCharacters(cmdContext, component.logicComponent.text, component.images[0]!!.image)
            component.logicComponent.textRequiresUpdate = false
        }
    }

    private fun blitCharacters(cmdContext: GraphicalUIVulkan.CommandBufferContext, text: String, dstImage: VulkanImage) {
        val glyphHeight = dstImage.config.extent.height
        val glyphWidth = (glyphHeight / glyphAtlas.glyphHeight) * glyphAtlas.glyphWidth
        val blitRegions = mutableListOf<ImageBlit>()
        val maxX = dstImage.config.extent.width

        repeat(text.length) { i ->
            val glyphBounds = glyphAtlas.getGlyphBounds(text[i])

            val dstMin = IVec2(min(i * glyphWidth, maxX), 0)
            val dstMax = IVec2(min(i * glyphWidth + glyphWidth, maxX), glyphHeight)

            val region = ImageBlit(
                ImageRegion(
                    Offset3D(glyphBounds.min.x, glyphBounds.min.y, 0),
                    Offset3D(glyphBounds.max.x, glyphBounds.max.y, 1)
                ),
                ImageRegion(
                    Offset3D(dstMin.x, dstMin.y, 0),
                    Offset3D(dstMax.x, dstMax.y, 1)
                )
            )
            blitRegions.add(region)
        }

        cmdContext.transitionImageLayout(
            dstImage,
            AccessMask.SHADER_READ, AccessMask.TRANSFER_WRITE,
            ImageLayout.SHADER_READ_ONLY_OPTIMAL, ImageLayout.TRANSFER_DST_OPTIMAL,
            PipelineStage.FRAGMENT_SHADER, PipelineStage.TRANSFER
        )

        cmdContext.clearColorImage(dstImage, Vec4(0f))

        cmdContext.blitImage(glyphAtlasImage, dstImage, blitRegions)

        cmdContext.transitionImageLayout(
            dstImage,
            AccessMask.TRANSFER_WRITE, AccessMask.SHADER_READ,
            ImageLayout.TRANSFER_DST_OPTIMAL, ImageLayout.SHADER_READ_ONLY_OPTIMAL,
            PipelineStage.TRANSFER, PipelineStage.FRAGMENT_SHADER
        )
    }


    override fun destroy() {
        super.destroy()
        images.forEach { it.image.destroy() }
        screenSizeBuffers.forEach { it.destroy() }
        glyphAtlasImage.destroy()
        vertexBuffer.destroy()
        indexBuffer.destroy()
        sampler.destroy(device)
        descriptorPool.destroy(device)
        descriptorSetLayout.destroy(device)
        imagesDescriptorSetLayout.destroy(device)
        pipeline.destroy(device)
        componentRenderers.clear()
    }


    companion object {
        private const val MAX_IMAGE_COUNT = 64
    }
}