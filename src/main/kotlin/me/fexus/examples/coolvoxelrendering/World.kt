package me.fexus.examples.coolvoxelrendering

import me.fexus.examples.coolvoxelrendering.misc.CommandRecorder
import me.fexus.examples.coolvoxelrendering.misc.DescriptorFactory
import me.fexus.examples.coolvoxelrendering.world.Chunk
import me.fexus.examples.coolvoxelrendering.world.ChunkHullingThread
import me.fexus.examples.coolvoxelrendering.world.Direction
import me.fexus.examples.coolvoxelrendering.world.TerrainGeneratorGPU
import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHull
import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHullingPacket
import me.fexus.examples.coolvoxelrendering.world.rendering.WorldRenderer
import me.fexus.math.repeat3D
import me.fexus.math.vec.IVec3
import me.fexus.voxel.VoxelOctree
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.component.CommandBuffer
import java.util.concurrent.ConcurrentLinkedQueue


class World(private val deviceUtil: VulkanDeviceUtil, private val descriptorFactory: DescriptorFactory): CommandRecorder {
    private val terrainGenerator = TerrainGeneratorGPU(deviceUtil, descriptorFactory)
    private val unsubmittedChunks = mutableMapOf<IVec3, Chunk>()
    private val submittedChunks = mutableMapOf<IVec3, Chunk>()

    private val chunkHullingInputQueue = ConcurrentLinkedQueue<ChunkHullingPacket>()
    private val chunkHullingOutputQueue = ConcurrentLinkedQueue<ChunkHull>()
    private val hullingThreads = Array(1) { ChunkHullingThread(chunkHullingInputQueue, chunkHullingOutputQueue) }

    private val renderer = WorldRenderer(deviceUtil, descriptorFactory)


    fun init() {
        renderer.init()
        terrainGenerator.init()

        hullingThreads.forEach(Thread::start)

        val chunksToGenerate = mutableListOf<IVec3>()
        val horizontal = 20
        val vertical = 10
        repeat3D(horizontal,vertical,horizontal) { x, y, z ->
            chunksToGenerate.add(IVec3(x,y,z) - (IVec3(horizontal, vertical, horizontal) ushr 1))
        }
        terrainGenerator.submitChunksForGeneration(chunksToGenerate)
    }


    override fun recordComputeCommands(commandBuffer: CommandBuffer, frameIndex: Int) {
        renderer.recordComputeCommands(commandBuffer)
        terrainGenerator.recordComputeCommands(commandBuffer, frameIndex)

        terrainGenerator.getFinishedChunks().forEach { unsubmittedChunks[it.position] = it }

        val chunksSubmitted = mutableListOf<Chunk>()
        unsubmittedChunks.values.forEach {
            val surroundingChunks = mutableListOf<Chunk>()
            for (dir in Direction.DIRECTIONS) {
                val nextChunk = getChunkAt(it.position + dir.normal) ?: return@forEach
                surroundingChunks.add(nextChunk)
            }
            chunksSubmitted.add(it)
            val depth = VoxelOctree.MAX_DEPTH
            chunkHullingInputQueue.add(ChunkHullingPacket(it, surroundingChunks, depth))
        }
        chunksSubmitted.forEach {
            unsubmittedChunks.remove(it.position)
            submittedChunks[it.position] = it
        }

        val newlyHulledChunks: List<ChunkHull> = List(chunkHullingOutputQueue.size) { chunkHullingOutputQueue.poll() }
        newlyHulledChunks.forEach { if (it.data.instanceCount > 0) renderer.submitChunkHull(it) }
    }

    override fun recordGraphicsCommands(commandBuffer: CommandBuffer, frameIndex: Int) {
        renderer.recordRenderCommands(commandBuffer)
    }


    fun getChunkAt(pos: IVec3): Chunk? {
        return unsubmittedChunks[pos] ?: submittedChunks[pos]
    }


    fun destroy() {
        hullingThreads.forEach { it.shouldRun.set(false) }
        renderer.destroy()
        terrainGenerator.destroy()
    }
}