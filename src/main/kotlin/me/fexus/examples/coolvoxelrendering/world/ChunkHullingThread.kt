package me.fexus.examples.coolvoxelrendering.world

import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHull
import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHullFactory
import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHullingPacket
import me.fexus.voxel.VoxelOctree
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean


class ChunkHullingThread(
    private val inputQueue: ConcurrentLinkedQueue<ChunkHullingPacket>,
    private val outputQueue: ConcurrentLinkedQueue<ChunkHull>
): Thread() {
    private val chunkHullFactory = ChunkHullFactory()
    val shouldRun: AtomicBoolean = AtomicBoolean(true)

    override fun run() {
        while (shouldRun.get()) {
            val packet = inputQueue.poll()

            if (packet != null) {
                val hullData = chunkHullFactory.buildSimple(packet)
                val hull = ChunkHull(packet.chunk.position, hullData)
                outputQueue.add(hull)
                continue
            }

            sleep(20)
        }
    }
}