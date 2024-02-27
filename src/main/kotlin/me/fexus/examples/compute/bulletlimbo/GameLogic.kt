package me.fexus.examples.compute.bulletlimbo

import me.fexus.camera.Camera2D
import me.fexus.examples.Globals
import me.fexus.math.vec.Vec2
import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.component.CommandBuffer
import me.fexus.vulkan.component.Device
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
import me.fexus.vulkan.component.pipeline.*
import me.fexus.vulkan.component.pipeline.shaderstage.ShaderStage
import me.fexus.vulkan.component.pipeline.specializationconstant.SpecializationConstantInt
import me.fexus.vulkan.descriptors.DescriptorType
import me.fexus.vulkan.descriptors.buffer.*
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlag
import me.fexus.vulkan.util.ImageExtent2D
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*


// Gamelogic is mostly process on the GPU side, hence we have some command buffer logic in here as well.
// We do have to check for signals from the GPU after all.
class GameLogic {
    companion object {
        private const val BULLETS_PER_BUFFER = 4096
        private const val STORAGE_BUFFER_ARRAY_SIZE = 8
        private const val WORKGROUP_SIZE_X = 64
    }

    private lateinit var deviceUtil: VulkanDeviceUtil
    private val device: Device; get() = deviceUtil.device

    private val camera = Camera2D(Vec2(0f), Vec2(16f, 9f))
    private var tickCounter: Long = 0L
    private val playArea = Area2D(Vec2(0f), Vec2(16f, 9f))

    private val spriteMesh = SpriteMesh()

    private val descriptorPool = DescriptorPool()
    private val descriptorSetLayout = DescriptorSetLayout()
    private val descriptorSets = Array(Globals.FRAMES_TOTAL) { DescriptorSet() }
    private val graphicsPipeline = GraphicsPipeline()
    private val computePipeline = ComputePipeline()

    private val gameBuffersBucket = BufferBucket()
    private val worldInfoBuffer = WorldInfoBuffer() // contains camera and time
    private lateinit var inputMapBuffers: Array<VulkanBuffer> // contains current inputs
    private lateinit var playerInfoBuffer: VulkanBuffer // contains player info (health, status)
    private lateinit var levelTimelineBuffer: VulkanBuffer // contains time-dependent references to the eventbuffer
    private lateinit var levelTimelineEventsBuffer: VulkanBuffer // contains level events such as bullet or enemy spawns
    private lateinit var enemyBehaviourBuffer: VulkanBuffer // contains enemy flight routes and shoot events
    private lateinit var signalBuffers: Array<VulkanBuffer> // contains flags so that the GPU can signal certain events
    private val bulletDataBuffers = mutableListOf<VulkanBuffer>() // contains bullet data such as position, rotation, visual...


    fun init(deviceUtil: VulkanDeviceUtil) {
        this.deviceUtil = deviceUtil

        spriteMesh.init(deviceUtil)
        createObjects()
        createDescriptorStuff()
        createPipelines()
    }


    private fun updateBuffers(frameIndex: Int) {
        worldInfoBuffer.updateData(camera.position, camera.extent, tickCounter, playArea, frameIndex)

        updateDescriptorSet(frameIndex)
    }

    fun recordGameLogicCompute(commandBuffer: CommandBuffer, frameIndex: Int) = runMemorySafe {
        // synchronize the read and write ops
        val computeBarrier = calloc(VkBufferMemoryBarrier::calloc, 1)
        with(computeBarrier[0]) {
            sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
            pNext(0L)
            buffer(bulletDataBuffers[frameIndex].vkBufferHandle)
            srcAccessMask(VK_ACCESS_SHADER_READ_BIT)
            dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            offset(0L)
            size(VK_WHOLE_SIZE)
        }
        vkCmdPipelineBarrier(
            commandBuffer.vkHandle,
            VK_PIPELINE_STAGE_VERTEX_SHADER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
            0, null, computeBarrier, null
        )

        updateBuffers(frameIndex)

        val pDescriptorSets = allocateLongValues(descriptorSets[frameIndex].vkHandle)

        val pPushConstants = allocate(128)
        pPushConstants.putInt(0, 0)
        pPushConstants.putInt(4, 1)
        pPushConstants.putInt(8, frameIndex)

        vkCmdBindDescriptorSets(
            commandBuffer.vkHandle,
            VK_PIPELINE_BIND_POINT_COMPUTE,
            computePipeline.vkLayoutHandle,
            0,
            pDescriptorSets,
            null
        )
        vkCmdBindPipeline(commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.vkHandle)
        vkCmdPushConstants(
            commandBuffer.vkHandle,
            computePipeline.vkLayoutHandle,
            VK_SHADER_STAGE_COMPUTE_BIT or VK_SHADER_STAGE_FRAGMENT_BIT or VK_SHADER_STAGE_VERTEX_BIT,
            0,
            pPushConstants
        )
        vkCmdDispatch(commandBuffer.vkHandle, BULLETS_PER_BUFFER / WORKGROUP_SIZE_X, 1, 1)

        with(computeBarrier[0]) {
            srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
            dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
        }
        vkCmdPipelineBarrier(
            commandBuffer.vkHandle,
            VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_VERTEX_SHADER_BIT,
            0, null, computeBarrier, null
        )
        // COMPUTE
    }


    fun recordDrawCommands(commandBuffer: CommandBuffer, frameIndex: Int) = runMemorySafe {

        val pDescriptorSets = allocateLongValues(descriptorSets[frameIndex].vkHandle)

        val pPushConstants = allocate(128)

        val pVertexBuffers = allocateLong(1)
        pVertexBuffers.put(0, spriteMesh.vertexBuffer.vkBufferHandle)
        val pOffsets = allocateLong(1)
        pOffsets.put(0, 0L)

        vkCmdBindDescriptorSets(
            commandBuffer.vkHandle,
            VK_PIPELINE_BIND_POINT_GRAPHICS,
            graphicsPipeline.vkLayoutHandle,
            0,
            pDescriptorSets,
            null
        )
        vkCmdBindPipeline(commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline.vkHandle)
        vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
        vkCmdBindIndexBuffer(commandBuffer.vkHandle, spriteMesh.indexBuffer.vkBufferHandle, 0L, VK_INDEX_TYPE_UINT32)
        vkCmdPushConstants(
            commandBuffer.vkHandle,
            graphicsPipeline.vkLayoutHandle,
            VK_SHADER_STAGE_COMPUTE_BIT or VK_SHADER_STAGE_FRAGMENT_BIT or VK_SHADER_STAGE_VERTEX_BIT,
            0,
            pPushConstants
        )
        vkCmdDrawIndexed(commandBuffer.vkHandle, 6, BULLETS_PER_BUFFER, 0, 0, 0)

        tickCounter++
    }


    private fun updateDescriptorSet(frameIndex: Int) {
        val descriptorSet = this.descriptorSets[frameIndex]

        // Update Descriptor Set
        val descWriteCameraBuf = DescriptorBufferWrite(
            0, DescriptorType.UNIFORM_BUFFER, Globals.FRAMES_TOTAL, descriptorSet, 0,
            listOf(DescriptorBufferInfo(worldInfoBuffer[frameIndex].vkBufferHandle, 0L, VK_WHOLE_SIZE))
        )
        val descWriteBufferArr = DescriptorBufferWrite(
            1, DescriptorType.STORAGE_BUFFER, bulletDataBuffers.size, descriptorSet, 0,
            bulletDataBuffers.map {
                DescriptorBufferInfo(it.vkBufferHandle, 0L, VK_WHOLE_SIZE)
            }
        )
        descriptorSet.update(device, descWriteCameraBuf, descWriteBufferArr)
    }

    private fun createDescriptorStuff() {
        // Descriptor Sets and Pipeline
        val poolPlan = DescriptorPoolPlan(
            Globals.FRAMES_TOTAL, DescriptorPoolCreateFlag.FREE_DESCRIPTOR_SET,
            listOf(
                DescriptorPoolSize(DescriptorType.UNIFORM_BUFFER, 16),
                DescriptorPoolSize(DescriptorType.STORAGE_BUFFER, 64),
                DescriptorPoolSize(DescriptorType.SAMPLED_IMAGE, 16),
                DescriptorPoolSize(DescriptorType.SAMPLER, 16)
            )
        )
        this.descriptorPool.create(device, poolPlan)

        val setLayoutPlan = DescriptorSetLayoutPlan(
            DescriptorSetLayoutCreateFlag.NONE,
            listOf(
                DescriptorSetLayoutBinding(
                    0, Globals.FRAMES_TOTAL,
                    DescriptorType.UNIFORM_BUFFER,
                    ShaderStage.COMPUTE + ShaderStage.VERTEX + ShaderStage.FRAGMENT,
                    DescriptorSetLayoutBindingFlag.NONE
                ),
                DescriptorSetLayoutBinding(
                    1, STORAGE_BUFFER_ARRAY_SIZE,
                    DescriptorType.STORAGE_BUFFER,
                    ShaderStage.COMPUTE + ShaderStage.VERTEX + ShaderStage.FRAGMENT,
                    DescriptorSetLayoutBindingFlag.PARTIALLY_BOUND
                )
            )
        )
        this.descriptorSetLayout.create(device, setLayoutPlan)

        this.descriptorSets.forEach {
            it.create(device, descriptorPool, descriptorSetLayout)
        }
    }

    private fun createPipelines() {
        val computePipelineConfig = ComputePipelineConfiguration(
            ClassLoader.getSystemResource("shaders/compute/particle_compute.spv").readBytes(),
            pushConstantsLayout = PushConstantsLayout(128, shaderStages = ShaderStage.COMPUTE + ShaderStage.BOTH),
            specializationConstants = listOf(
                SpecializationConstantInt(0, STORAGE_BUFFER_ARRAY_SIZE),
                SpecializationConstantInt(1, WORKGROUP_SIZE_X),
                SpecializationConstantInt(2, Globals.FRAMES_TOTAL)
            )
        )
        this.computePipeline.create(deviceUtil.device, descriptorSetLayout, computePipelineConfig)

        val pipelineConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC4, 0),
                VertexAttribute(1, VertexAttributeFormat.VEC4, 16),
            ),
            PushConstantsLayout(128, shaderStages = ShaderStage.COMPUTE + ShaderStage.BOTH),
            ClassLoader.getSystemResource("shaders/compute/standard_vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/compute/standard_frag.spv").readBytes(),
            dynamicStates = listOf(DynamicState.VIEWPORT, DynamicState.SCISSOR),
            blendEnable = true,
            specializationConstants = listOf(
                SpecializationConstantInt(0, STORAGE_BUFFER_ARRAY_SIZE),
                SpecializationConstantInt(1, Globals.FRAMES_TOTAL)
            )
        )
        this.graphicsPipeline.create(device, listOf(descriptorSetLayout), pipelineConfig)
    }


    private fun createObjects() {
        worldInfoBuffer.init(deviceUtil)

        val inputMapBufferConfig = VulkanBufferConfiguration(
            64L,
            MemoryPropertyFlag.HOST_VISIBLE + MemoryPropertyFlag.HOST_COHERENT,
            BufferUsage.STORAGE_BUFFER
        )
        this.inputMapBuffers = Array(Globals.FRAMES_TOTAL) {
            deviceUtil.createBuffer(inputMapBufferConfig) inside gameBuffersBucket
        }

        val playerInfoBufferConfig = VulkanBufferConfiguration(
            64L,
            MemoryPropertyFlag.DEVICE_LOCAL,
            BufferUsage.STORAGE_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.playerInfoBuffer = deviceUtil.createBuffer(playerInfoBufferConfig) inside gameBuffersBucket

        val levelTimelineBufferConfig = VulkanBufferConfiguration(
            64L,
            MemoryPropertyFlag.DEVICE_LOCAL,
            BufferUsage.STORAGE_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.levelTimelineBuffer = deviceUtil.createBuffer(levelTimelineBufferConfig) inside gameBuffersBucket

        val levelTimelineEventsBufferConfig = VulkanBufferConfiguration(
            64L,
            MemoryPropertyFlag.DEVICE_LOCAL,
            BufferUsage.STORAGE_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.levelTimelineEventsBuffer =
            deviceUtil.createBuffer(levelTimelineEventsBufferConfig) inside gameBuffersBucket

        val signalBufferConfig = VulkanBufferConfiguration(
            64L,
            MemoryPropertyFlag.HOST_VISIBLE + MemoryPropertyFlag.HOST_COHERENT,
            BufferUsage.STORAGE_BUFFER
        )
        this.signalBuffers = Array(Globals.FRAMES_TOTAL) {
            deviceUtil.createBuffer(signalBufferConfig) inside gameBuffersBucket
        }

        val enemyBehaviourBufferConfig = VulkanBufferConfiguration(
            64L,
            MemoryPropertyFlag.DEVICE_LOCAL,
            BufferUsage.STORAGE_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.enemyBehaviourBuffer = deviceUtil.createBuffer(enemyBehaviourBufferConfig) inside gameBuffersBucket

        val bulletBufferConfig = VulkanBufferConfiguration(
            32L * BULLETS_PER_BUFFER,
            MemoryPropertyFlag.DEVICE_LOCAL,
            BufferUsage.STORAGE_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.bulletDataBuffers.add(deviceUtil.createBuffer(bulletBufferConfig) inside gameBuffersBucket)
    }


    fun onResizeDestroy() {}

    fun onResizeRecreate(newExtent: ImageExtent2D) {}


    fun destroy() {
        computePipeline.destroy(device)
        gameBuffersBucket.destroyDescriptors().clear()
    }
}