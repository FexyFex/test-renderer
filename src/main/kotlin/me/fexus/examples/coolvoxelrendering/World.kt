package me.fexus.examples.coolvoxelrendering

import me.fexus.camera.CameraPerspective
import me.fexus.examples.coolvoxelrendering.misc.CommandRecorder
import me.fexus.examples.coolvoxelrendering.misc.DescriptorFactory
import me.fexus.examples.coolvoxelrendering.world.Chunk
import me.fexus.examples.coolvoxelrendering.world.ChunkHullingThread
import me.fexus.examples.coolvoxelrendering.world.Direction
import me.fexus.examples.coolvoxelrendering.world.generation.TerrainGeneratorGPU
import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHull
import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHullingPacket
import me.fexus.examples.coolvoxelrendering.world.generation.BlockTypePlacementVerifier
import me.fexus.examples.coolvoxelrendering.world.position.ChunkPosition
import me.fexus.examples.coolvoxelrendering.world.WorldRenderer
import me.fexus.math.repeat3D
import me.fexus.math.vec.IVec3
import me.fexus.voxel.VoxelOctree
import me.fexus.voxel.VoxelRegistry
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.component.CommandBuffer
import java.util.concurrent.ConcurrentLinkedQueue


class World(
    private val deviceUtil: VulkanDeviceUtil,
    private val descriptorFactory: DescriptorFactory,
    private val voxelRegistry: VoxelRegistry,
    private val camera: CameraPerspective,
): CommandRecorder {
    private val terrainGenerator = TerrainGeneratorGPU(deviceUtil, descriptorFactory)
    private val allChunks = mutableMapOf<ChunkPosition, Chunk>()
    private val candidateChunksForHulling = mutableMapOf<ChunkPosition, Chunk>()

    private val blockTypeVerifier = BlockTypePlacementVerifier()

    private val chunkHullingInputQueue = ConcurrentLinkedQueue<ChunkHullingPacket>()
    private val chunkHullingOutputQueue = ConcurrentLinkedQueue<ChunkHull>()
    private val hullingThreads = Array(1) { ChunkHullingThread(chunkHullingInputQueue, chunkHullingOutputQueue) }

    private val renderer = WorldRenderer(deviceUtil, descriptorFactory, camera)


    fun init() {
        renderer.init()
        terrainGenerator.init()

        hullingThreads.forEach(Thread::start)

        val chunksToGenerate = mutableListOf<ChunkPosition>()
        val horizontal = 32
        val vertical = 8
        repeat3D(horizontal,vertical,horizontal) { x, y, z ->
            chunksToGenerate.add(ChunkPosition(x - (horizontal ushr 1), (y - (vertical ushr 1)) * -1, z - (horizontal ushr 1)))
        }
        terrainGenerator.submitChunksForGeneration(chunksToGenerate)
    }


    override fun recordComputeCommands(commandBuffer: CommandBuffer, frameIndex: Int) {
        renderer.recordComputeCommands(commandBuffer, frameIndex)
        terrainGenerator.recordComputeCommands(commandBuffer, frameIndex)

        terrainGenerator.getFinishedChunks().forEach {
            allChunks[it.position] = it
            candidateChunksForHulling[it.position] = it
        }

        val newlyHulledChunks: List<ChunkHull> = List(chunkHullingOutputQueue.size) { chunkHullingOutputQueue.poll() }
        newlyHulledChunks.forEach { if (it.data.instanceCount > 0) renderer.submitChunkHull(it) }

        val chunksToSubmit = mutableListOf<ChunkHullingPacket>()
        val chunksToVerify = mutableMapOf<ChunkPosition, Chunk>()
        candidateChunksForHulling.values.forEach {
            val surroundingChunks = mutableListOf<Chunk>()
            for (dir in Direction.DIRECTIONS) {
                val nextChunk = getChunkAt(dir.normal + it.position) ?: return@forEach
                surroundingChunks.add(nextChunk)
            }
            val depth = VoxelOctree.MAX_DEPTH
            chunksToSubmit.add(ChunkHullingPacket(it, surroundingChunks, depth))
            if (it.isSoilFlagged) chunksToVerify[it.position] = it
        }

        blockTypeVerifier.verifySoil(allChunks, chunksToVerify)

        chunksToSubmit.forEach {
            chunkHullingInputQueue.add(it)
            candidateChunksForHulling.remove(it.chunk.position)
        }
    }

    override fun recordGraphicsCommands(commandBuffer: CommandBuffer, frameIndex: Int) {
        renderer.recordRenderCommands(commandBuffer, frameIndex)
    }


    fun getChunkAt(pos: IVec3) = getChunkAt(ChunkPosition(pos))
    fun getChunkAt(pos: ChunkPosition): Chunk? {
        return allChunks[pos]
    }


    fun destroy() {
        hullingThreads.forEach { it.shouldRun.set(false) }
        renderer.destroy()
        terrainGenerator.destroy()
    }
}