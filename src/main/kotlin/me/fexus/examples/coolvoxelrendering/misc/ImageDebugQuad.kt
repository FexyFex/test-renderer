package me.fexus.examples.coolvoxelrendering.misc

import me.fexus.examples.surroundsound.MeshUploader
import me.fexus.model.QuadModelTriangleStrips
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.component.pipeline.*
import me.fexus.vulkan.component.pipeline.shaderstage.ShaderStage
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.image.VulkanImage
import java.nio.ByteBuffer
import java.nio.ByteOrder


class ImageDebugQuad(private val deviceUtil: VulkanDeviceUtil, private val descriptorFactory: DescriptorFactory) {
    lateinit var vertexBuffer: VulkanBuffer; private set
    val pipeline = GraphicsPipeline()


    fun init() {
        val meshUploader = MeshUploader(deviceUtil)

        val size = QuadModelTriangleStrips.vertices.size * QuadModelTriangleStrips.Vertex.SIZE_BYTES
        val buf = ByteBuffer.allocate(size)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        QuadModelTriangleStrips.vertices.forEachIndexed { vIndex, vertex ->
            val offset = vIndex * QuadModelTriangleStrips.Vertex.SIZE_BYTES
            vertex.writeToByteBuffer(buf, offset)
        }
        this.vertexBuffer = meshUploader.uploadBuffer(buf, BufferUsage.VERTEX_BUFFER)

        val pipelineConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC3, 0),
                VertexAttribute(1, VertexAttributeFormat.VEC2, 12)
            ),
            PushConstantsLayout(128, shaderStages = ShaderStage.COMPUTE + ShaderStage.FRAGMENT + ShaderStage.VERTEX),
            ClassLoader.getSystemResource("shaders/coolvoxelrendering/debugquad/debugquad_vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/coolvoxelrendering/debugquad/debugquad_frag.spv").readBytes(),
            dynamicStates = listOf(DynamicState.SCISSOR, DynamicState.VIEWPORT),
            primitive = Primitive.TRIANGLE_STRIPS,
            cullMode = CullMode.NONE, depthTest = false, depthWrite = false
        )
        this.pipeline.create(deviceUtil.device, listOf(descriptorFactory.descriptorSetLayout), pipelineConfig)
    }


    fun destroy() {
        pipeline.destroy()
        vertexBuffer.destroy()
    }
}