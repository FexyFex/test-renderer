package me.fexus.examples.coolvoxelrendering.world

import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHull
import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHullFactory
import me.fexus.voxel.VoxelOctree
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean


class ChunkHullingThread(
    private val inputQueue: ConcurrentLinkedQueue<Chunk>,
    private val outputQueue: ConcurrentLinkedQueue<ChunkHull>
): Thread() {
    private val chunkHullFactory = ChunkHullFactory()
    val shouldRun: AtomicBoolean = AtomicBoolean(true)

    override fun run() {
        while (shouldRun.get()) {
            val chunk = inputQueue.poll()

            if (chunk != null) {
                val hullData = chunkHullFactory.buildSimple(chunk, VoxelOctree.MAX_DEPTH)
                val hull = ChunkHull(chunk.position, hullData)
                outputQueue.add(hull)
            }



            sleep(1)
        }
    }
}