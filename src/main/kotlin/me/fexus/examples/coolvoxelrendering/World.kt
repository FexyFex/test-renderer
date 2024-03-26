package me.fexus.examples.coolvoxelrendering

import me.fexus.examples.coolvoxelrendering.world.Chunk
import me.fexus.examples.coolvoxelrendering.world.ChunkHullingThread
import me.fexus.examples.coolvoxelrendering.world.TerrainGeneratorGPU
import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHull
import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHullingPacket
import me.fexus.examples.coolvoxelrendering.world.rendering.WorldRenderer
import me.fexus.math.repeat3D
import me.fexus.math.repeatCubed
import me.fexus.math.vec.IVec3
import me.fexus.voxel.VoxelOctree
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.component.CommandBuffer
import java.util.concurrent.ConcurrentLinkedQueue


class World(private val deviceUtil: VulkanDeviceUtil, private val descriptorFactory: DescriptorFactory): CommandRecorder {
    private val terrainGenerator = TerrainGeneratorGPU(deviceUtil, descriptorFactory)
    private val chunks = mutableMapOf<IVec3, Chunk>()

    private val chunkHullingInputQueue = ConcurrentLinkedQueue<ChunkHullingPacket>()
    private val chunkHullingOutputQueue = ConcurrentLinkedQueue<ChunkHull>()
    private val hullingThreads = Array(1) { ChunkHullingThread(chunkHullingInputQueue, chunkHullingOutputQueue) }

    private val renderer = WorldRenderer(deviceUtil, descriptorFactory)


    fun init() {
        renderer.init()
        terrainGenerator.init()

        hullingThreads.forEach(Thread::start)

        val chunksToGenerate = mutableListOf<IVec3>()
        repeat3D(24, 12, 24) { x, y, z ->
            chunksToGenerate.add(IVec3(x,y,z) - IVec3(12, 6, 12))
        }
        terrainGenerator.submitChunksForGeneration(chunksToGenerate)
    }


    override fun recordComputeCommands(commandBuffer: CommandBuffer, frameIndex: Int) {
        renderer.recordComputeCommands(commandBuffer)
        terrainGenerator.recordComputeCommands(commandBuffer, frameIndex)

        terrainGenerator.getFinishedChunks().forEach { chunks[it.position] = it }

        val submittableChunks = chunks.values.filter { !it.submittedForHulling }
        submittableChunks.forEach {
            val surroundingChunks = mutableListOf<Chunk>()
            for (dir in Direction.DIRECTIONS) {
                val nextChunk = chunks[it.position + dir.normal] ?: return@forEach
                surroundingChunks.add(nextChunk)
            }
            it.submittedForHulling = true
            val depth = if (it.position.length > 16) VoxelOctree.MAX_DEPTH - 1 else VoxelOctree.MAX_DEPTH
            chunkHullingInputQueue.add(ChunkHullingPacket(it, surroundingChunks, depth))
        }

        val newlyHulledChunks: List<ChunkHull> = List(chunkHullingOutputQueue.size) { chunkHullingOutputQueue.poll() }
        newlyHulledChunks.forEach { if (it.data.instanceCount > 0) renderer.submitChunkHull(it) }
    }

    override fun recordGraphicsCommands(commandBuffer: CommandBuffer, frameIndex: Int) {
        renderer.recordRenderCommands(commandBuffer)
    }


    fun destroy() {
        hullingThreads.forEach { it.shouldRun.set(false) }
        renderer.destroy()
        terrainGenerator.destroy()
    }
}