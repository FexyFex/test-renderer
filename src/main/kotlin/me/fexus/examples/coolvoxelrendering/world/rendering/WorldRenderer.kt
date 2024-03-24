package me.fexus.examples.coolvoxelrendering.world.rendering

import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHullFactory
import me.fexus.examples.coolvoxelrendering.world.chunk.hull.ChunkHullData
import me.fexus.examples.surroundsound.MeshUploader
import me.fexus.memory.runMemorySafe
import me.fexus.model.QuadModelTriangleStrips
import me.fexus.voxel.VoxelOctree
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.component.CommandBuffer
import me.fexus.vulkan.component.descriptor.set.layout.DescriptorSetLayout
import me.fexus.vulkan.component.pipeline.*
import me.fexus.vulkan.component.pipeline.shaderstage.ShaderStage
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlag
import org.lwjgl.vulkan.VK12.*
import sun.security.ec.point.ProjectivePoint.Mutable
import java.nio.ByteBuffer
import java.nio.ByteOrder


class WorldRenderer(private val deviceUtil: VulkanDeviceUtil) {
    private val chunkHullFactory = ChunkHullFactory()
    private val hullPipeline = GraphicsPipeline()
    private lateinit var hullVertexBuffer: VulkanBuffer
    lateinit var hullPositionBuffers: VulkanBuffer

    private val chunkHulls = mutableListOf<ChunkHullData>()


    fun init(descriptorSetLayout: DescriptorSetLayout) {
        createHullBuffers()
        createHullPipeline(descriptorSetLayout)
    }


    fun recordRenderCommands(commandBuffer: CommandBuffer) = runMemorySafe {
        val pVertexBuffers = allocateLongValues(hullVertexBuffer.vkBufferHandle)
        val pOffsets = allocateLongValues(0)

        val pPushConstants = allocate(128)
        pPushConstants.putInt(0, 0)


        vkCmdBindPipeline(commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, hullPipeline.vkHandle)
        vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
        vkCmdPushConstants(
            commandBuffer.vkHandle,
            hullPipeline.vkLayoutHandle,
            ShaderStage.BOTH.vkBits,
            0,
            pPushConstants
        )
        vkCmdDraw(commandBuffer.vkHandle, 4, sideInstanceCount, 0, 0)
    }


    private fun createHullBuffers() {
        val meshUploader = MeshUploader(deviceUtil)

        val size = QuadModelTriangleStrips.vertices.size * QuadModelTriangleStrips.Vertex.SIZE_BYTES
        val buf = ByteBuffer.allocate(size)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        QuadModelTriangleStrips.vertices.forEachIndexed { vIndex, vertex ->
            val offset = vIndex * QuadModelTriangleStrips.Vertex.SIZE_BYTES
            vertex.writeToByteBuffer(buf, offset)
        }
        this.hullVertexBuffer = meshUploader.uploadBuffer(buf, BufferUsage.VERTEX_BUFFER)

        val positionsBufferConfig = VulkanBufferConfiguration(
            VoxelOctree.VOXEL_COUNT * 6L * Int.SIZE_BYTES,
            MemoryPropertyFlag.HOST_VISIBLE + MemoryPropertyFlag.HOST_COHERENT,
            BufferUsage.STORAGE_BUFFER
        )
        this.hullPositionBuffers = deviceUtil.createBuffer(positionsBufferConfig)
    }

    private fun createHullPipeline(descriptorSetLayout: DescriptorSetLayout) {
        val config = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC3, 0),
                VertexAttribute(1, VertexAttributeFormat.VEC2, 12)
            ),
            PushConstantsLayout(128),
            ClassLoader.getSystemResource("shaders/coolvoxelrendering/side_vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/coolvoxelrendering/side_frag.spv").readBytes(),
            dynamicStates = listOf(DynamicState.SCISSOR, DynamicState.VIEWPORT),
            primitive = Primitive.TRINAGLE_STRIPS,
            cullMode = CullMode.BACKFACE
        )
        this.hullPipeline.create(deviceUtil.device, listOf(descriptorSetLayout), config)
    }


    fun destroy() {
        hullVertexBuffer.destroy()
        hullPipeline.destroy()
        hullPositionBuffers.destroy()
    }
}