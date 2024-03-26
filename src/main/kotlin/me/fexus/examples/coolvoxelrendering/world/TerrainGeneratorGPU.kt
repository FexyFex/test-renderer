package me.fexus.examples.coolvoxelrendering.world

import me.fexus.examples.Globals
import me.fexus.examples.coolvoxelrendering.CommandRecorder
import me.fexus.examples.coolvoxelrendering.DescriptorFactory
import me.fexus.math.repeatCubed
import me.fexus.math.vec.IVec3
import me.fexus.memory.runMemorySafe
import me.fexus.voxel.VoxelRegistry
import me.fexus.voxel.type.VoidVoxel
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.component.CommandBuffer
import me.fexus.vulkan.component.pipeline.ComputePipeline
import me.fexus.vulkan.component.pipeline.ComputePipelineConfiguration
import me.fexus.vulkan.component.pipeline.PushConstantsLayout
import me.fexus.vulkan.component.pipeline.shaderstage.ShaderStage
import me.fexus.vulkan.component.pipeline.specializationconstant.SpecializationConstantInt
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlag
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkBufferMemoryBarrier
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors


class TerrainGeneratorGPU(
    private val deviceUtil: VulkanDeviceUtil,
    private val descriptorFactory: DescriptorFactory,
): CommandRecorder {
    private val basicTerrainGenPipeline = ComputePipeline()
    private val blockTypeGenPipeline = ComputePipeline()
    private val caveGenPipeline = ComputePipeline()
    private lateinit var chunkDataBuffers: Array<VulkanBuffer>
    private val currentChunkPositions = Array(Globals.FRAMES_TOTAL) { mutableListOf<IVec3>() }

    private val chunksToGenerate = mutableListOf<IVec3>()
    private val finishedChunks = ConcurrentLinkedQueue<Chunk>()

    private val threadPool = Executors.newCachedThreadPool()


    fun init() {
        val device = deviceUtil.device
        val descriptorSetLayout = descriptorFactory.descriptorSetLayout

        val basicTerrainGenPipelineConfig = ComputePipelineConfiguration(
            ClassLoader.getSystemResource("shaders/coolvoxelrendering/compute/terrain_gen/stage1_comp.spv").readBytes(),
            PushConstantsLayout(128, shaderStages = ShaderStage.COMPUTE),
            listOf(
                SpecializationConstantInt(0, 16),
                SpecializationConstantInt(1, BYTES_PER_VOXEL)
            )
        )
        this.basicTerrainGenPipeline.create(device, descriptorSetLayout, basicTerrainGenPipelineConfig)

        val blockTypeGenPipelineConfig = ComputePipelineConfiguration(
            ClassLoader.getSystemResource("shaders/coolvoxelrendering/compute/terrain_gen/stage2_comp.spv").readBytes(),
            PushConstantsLayout(128, shaderStages = ShaderStage.COMPUTE),
            listOf(
                SpecializationConstantInt(0, 16),
                SpecializationConstantInt(1, BYTES_PER_VOXEL)
            )
        )
        this.blockTypeGenPipeline.create(device, descriptorSetLayout, blockTypeGenPipelineConfig)

        val caveGenPipelineConfig = ComputePipelineConfiguration(
            ClassLoader.getSystemResource("shaders/coolvoxelrendering/compute/terrain_gen/stage3_comp.spv").readBytes(),
            PushConstantsLayout(128, shaderStages = ShaderStage.COMPUTE),
            listOf(
                SpecializationConstantInt(0, 16),
                SpecializationConstantInt(1, BYTES_PER_VOXEL)
            )
        )
        this.caveGenPipeline.create(device, descriptorSetLayout, caveGenPipelineConfig)

        val chunkDataBufferConfig = VulkanBufferConfiguration(
            MAX_CHUNKS_PER_SUBMIT * Chunk.VOXELS_PER_CHUNK * BYTES_PER_VOXEL.toLong(),
            MemoryPropertyFlag.HOST_VISIBLE + MemoryPropertyFlag.HOST_COHERENT,
            BufferUsage.STORAGE_BUFFER + BufferUsage.TRANSFER_DST
        )
        chunkDataBuffers = descriptorFactory.createNBuffers(chunkDataBufferConfig)
        chunkDataBuffers.forEach { it.getInt(0) }
    }

    override fun recordComputeCommands(commandBuffer: CommandBuffer, frameIndex: Int) = runMemorySafe {
        val targetBuffer = chunkDataBuffers[frameIndex]

        pullChunksFromBuffer(frameIndex) // Retrieve the data first

        val chunkCount = if (chunksToGenerate.size > MAX_CHUNKS_PER_SUBMIT) MAX_CHUNKS_PER_SUBMIT else chunksToGenerate.size
        val targetChunks = List(chunkCount) { chunksToGenerate.removeAt(0) }
        currentChunkPositions[frameIndex].clear()
        currentChunkPositions[frameIndex].addAll(targetChunks)

        val pPushConstants = allocate(128)
        val pDescriptorSets = allocateLongValues(descriptorFactory.descriptorSets[frameIndex].vkHandle)

        vkCmdBindDescriptorSets(
            commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_COMPUTE, basicTerrainGenPipeline.vkLayoutHandle,
            0, pDescriptorSets, null
        )

        // BARRIER 0: PREPARE THE BUFFER FOR A CLEAR (TRANSFER OP)
        val barrier = calloc(VkBufferMemoryBarrier::calloc, 1)
        with(barrier[0]) {
            sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
            pNext(0)
            offset(0L)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            size(VK_WHOLE_SIZE)
            buffer(targetBuffer.vkBufferHandle)
            srcAccessMask(VK_ACCESS_SHADER_READ_BIT)
            dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
        }
        // BARRIER 0: PREPARE THE BUFFER FOR A CLEAR (TRANSFER OP)

        // STAGE 0: CLEAR THE BUFFER
        vkCmdFillBuffer(commandBuffer.vkHandle, targetBuffer.vkBufferHandle, 0L, VK_WHOLE_SIZE, 0)
        // STAGE 0: CLEAR THE BUFFER

        // BARRIER 1: MAKING SURE THE INITIAL WRITE ACCESS IS SAFE
        barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
        barrier.dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
        vkCmdPipelineBarrier(commandBuffer.vkHandle,
            VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
            0, null, barrier, null
        )
        // BARRIER 1: MAKING SURE THE INITIAL WRITE ACCESS IS SAFE

        // STAGE 1: GENERATING JUST THE TERRAIN (1 for a block or 0 air)
        vkCmdBindPipeline(commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_COMPUTE, basicTerrainGenPipeline.vkHandle)
        pPushConstants.putInt(0, targetBuffer.index)

        targetChunks.forEachIndexed { index, pos ->
            pPushConstants.putInt(4, index)
            pos.intoByteBuffer(pPushConstants, 8)
            vkCmdPushConstants(
                commandBuffer.vkHandle, basicTerrainGenPipeline.vkLayoutHandle,
                VK_SHADER_STAGE_COMPUTE_BIT, 0, pPushConstants
            )
            vkCmdDispatch(commandBuffer.vkHandle, WORK_GROUP_COUNT, WORK_GROUP_COUNT, WORK_GROUP_COUNT)
        }
        // STAGE 1: GENERATING JUST THE TERRAIN (1 for a block or 0 air)

        // BARRIER 2: WAITING FOR STAGE 1 TO COMPLETE
        barrier.srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
        barrier.dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT or VK_ACCESS_SHADER_READ_BIT)
        vkCmdPipelineBarrier(commandBuffer.vkHandle,
            VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
            0, null, barrier, null
        )
        // BARRIER 2: WAITING FOR STAGE 1 TO COMPLETE

        // STAGE 2: REPLACING THE STONE BLOCKS WITH OTHER VARIANTS
        vkCmdBindPipeline(commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_COMPUTE, blockTypeGenPipeline.vkHandle)
        targetChunks.forEachIndexed { index, pos ->
            pPushConstants.putInt(4, index)
            pos.intoByteBuffer(pPushConstants, 8)
            vkCmdPushConstants(
                commandBuffer.vkHandle, blockTypeGenPipeline.vkLayoutHandle,
                VK_SHADER_STAGE_COMPUTE_BIT, 0, pPushConstants
            )
            vkCmdDispatch(commandBuffer.vkHandle, WORK_GROUP_COUNT, WORK_GROUP_COUNT, WORK_GROUP_COUNT)
        }
        // STAGE 2: REPLACING THE STONE BLOCKS WITH OTHER VARIANTS

        // BARRIER 3: WAITING FOR STAGE 2 TO COMPLETE
        barrier.srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT or VK_ACCESS_SHADER_READ_BIT)
        barrier.dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT or VK_ACCESS_SHADER_READ_BIT)
        vkCmdPipelineBarrier(commandBuffer.vkHandle,
            VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
            0, null, barrier, null
        )
        // BARRIER 3: WAITING FOR STAGE 2 TO COMPLETE

        // STAGE 3: CARVING CAVES
        vkCmdBindPipeline(commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_COMPUTE, caveGenPipeline.vkHandle)
        targetChunks.forEachIndexed { index, pos ->
            pPushConstants.putInt(4, index)
            pos.intoByteBuffer(pPushConstants, 8)
            vkCmdPushConstants(
                commandBuffer.vkHandle, caveGenPipeline.vkLayoutHandle,
                VK_SHADER_STAGE_COMPUTE_BIT, 0, pPushConstants
            )
            vkCmdDispatch(commandBuffer.vkHandle, WORK_GROUP_COUNT, WORK_GROUP_COUNT, WORK_GROUP_COUNT)
        }
        // STAGE 3: CARVING CAVES

        // BARRIER 4: WAITING FOR STAGE 3 TO COMPLETE
        // BARRIER 4: WAITING FOR STAGE 3 TO COMPLETE

        // STAGE 4: ADDING DETAIL BLOCKS
        // STAGE 4: ADDING DETAIL BLOCKS

        // BARRIER 5: WAITING FOR STAGE 4 TO COMPLETE. END
        barrier.srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT or VK_ACCESS_SHADER_READ_BIT)
        barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
        vkCmdPipelineBarrier(commandBuffer.vkHandle,
            VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
            0, null, barrier, null
        )
        // BARRIER 5: WAITING FOR STAGE 4 TO COMPLETE. END
    }

    private fun pullChunksFromBuffer(bufferIndex: Int) {
        val buffer = chunkDataBuffers[bufferIndex]

        val tasks = currentChunkPositions[bufferIndex].mapIndexed { index, position ->
            Callable {
                val chunk = Chunk(position)
                val chunkOffset = index * BYTES_PER_CHUNK
                var isChunkFull = true

                repeatCubed(Chunk.EXTENT) { x, y, z ->
                    val offset = (x + (y * Chunk.EXTENT) + (z * Chunk.EXTENT * Chunk.EXTENT)) * BYTES_PER_VOXEL
                    val voxelID = buffer.getInt(chunkOffset + offset)
                    val voxel = VoxelRegistry.getVoxelByID(voxelID) ?: VoidVoxel
                    if (voxel == VoidVoxel) isChunkFull = false
                    chunk.setVoxelAt(x, y, z, voxel)
                }

                chunk.isFull = isChunkFull
                finishedChunks.add(chunk)
            }
        }

        val futures = threadPool.invokeAll(tasks)
        while (futures.any { !it.isDone }) { continue }
    }

    fun submitChunksForGeneration(chunkPositions: List<IVec3>) {
        chunksToGenerate.addAll(chunkPositions)
    }

    fun getFinishedChunks(): List<Chunk> {
        val size = finishedChunks.size
        return List(size) { finishedChunks.poll() }
    }


    fun destroy() {
        threadPool.shutdown()

        basicTerrainGenPipeline.destroy()
        blockTypeGenPipeline.destroy()
        caveGenPipeline.destroy()
        chunkDataBuffers.forEach(VulkanBuffer::destroy)
    }


    companion object {
        private const val MAX_CHUNKS_PER_SUBMIT = 12
        private const val BYTES_PER_VOXEL = 4
        private const val BYTES_PER_CHUNK = BYTES_PER_VOXEL * Chunk.VOXELS_PER_CHUNK

        private const val WORK_GROUP_COUNT = 2
    }
}