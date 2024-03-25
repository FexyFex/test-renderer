package me.fexus.examples.coolvoxelrendering

import me.fexus.examples.coolvoxelrendering.world.Chunk
import me.fexus.examples.coolvoxelrendering.world.ChunkHullingThread
import me.fexus.examples.coolvoxelrendering.world.TerrainGenerator
import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHull
import me.fexus.examples.coolvoxelrendering.world.rendering.WorldRenderer
import me.fexus.math.repeatCubed
import me.fexus.math.vec.IVec3
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.component.CommandBuffer
import java.util.concurrent.ConcurrentLinkedQueue


class World(private val deviceUtil: VulkanDeviceUtil, private val descriptorFactory: DescriptorFactory): CommandRecorder {
    private val chunkHullingInputQueue = ConcurrentLinkedQueue<Chunk>()
    private val chunkHullingOutputQueue = ConcurrentLinkedQueue<ChunkHull>()
    private val renderer = WorldRenderer(deviceUtil, descriptorFactory)
    private val terrainGenerator = TerrainGenerator(deviceUtil, descriptorFactory)

    private val hullingThread = ChunkHullingThread(chunkHullingInputQueue, chunkHullingOutputQueue)


    fun init() {
        renderer.init()
        terrainGenerator.init()

        val chunksToGenerate = mutableListOf<IVec3>()
        repeatCubed(4) { x, y, z ->
            chunksToGenerate.add(IVec3(x,y,z))
        }
        terrainGenerator.submitChunksForGeneration(chunksToGenerate)

        hullingThread.start()
    }


    override fun recordComputeCommands(commandBuffer: CommandBuffer, frameIndex: Int) {
        renderer.recordComputeCommands(commandBuffer)
        terrainGenerator.recordComputeCommands(commandBuffer, frameIndex)

        val finishedChunks = terrainGenerator.getFinishedChunks()
        finishedChunks.forEach { chunkHullingInputQueue.add(it) }

        val newlyHulledChunks: List<ChunkHull> = List(chunkHullingOutputQueue.size) { chunkHullingOutputQueue.poll() }
        newlyHulledChunks.forEach { renderer.submitChunkHull(it) }
    }

    override fun recordGraphicsCommands(commandBuffer: CommandBuffer, frameIndex: Int) {
        renderer.recordRenderCommands(commandBuffer)
    }


    fun destroy() {
        hullingThread.shouldRun.set(false)
        renderer.destroy()
        terrainGenerator.destroy()
    }
}