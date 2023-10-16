package me.fexus.examples.parallaxvoxelraytracing.buffer

import me.fexus.math.vec.IVec3
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferFactory
import me.fexus.vulkan.descriptors.buffer.VulkanBufferLayout
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperty
import java.nio.ByteBuffer


class ChunkDataBufferArray {
    companion object {
        const val BUFFER_SIZE = 256_000_000
    }

    private lateinit var bufferFactory: VulkanBufferFactory
    private val buffers = mutableListOf<ChunkBuffer>()
    lateinit var addressBuffer: VulkanBuffer
    var bufferArrayChanged: Boolean = false
    private lateinit var renderDistance: IVec3
    var chunkAddressOffset: IVec3 = IVec3(0,0,0)


    fun init(bufferFactory: VulkanBufferFactory, renderDistance: IVec3) {
        this.bufferFactory = bufferFactory
        val addressBufferLayout = VulkanBufferLayout(
            ((renderDistance.x * 2 + 1) * (renderDistance.y * 2 + 1) * (renderDistance.z.toLong() * 2 + 1)) * Int.SIZE_BYTES,
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.STORAGE_BUFFER
        )
        this.addressBuffer = bufferFactory.createBuffer(addressBufferLayout)
        this.addressBuffer.set(0, -1, addressBufferLayout.size)

        this.renderDistance = renderDistance
    }

    fun changeRenderDistance(renderDistance: IVec3) {

        this.renderDistance = renderDistance
    }

    fun putChunkData(chunkPos: IVec3, data: ByteBuffer): ChunkBufferAddress {
        var targetBuf: ChunkBuffer? = null
        var offset = -1
        var index = 0
        while (true) {
            if (index >= buffers.size) {
                targetBuf = createNewBuffer()
                offset = targetBuf.allocate(data)!!
                break
            }
            val currBuf = buffers[index++]
            val lOffset = currBuf.allocate(data) ?: continue
            offset = lOffset
            targetBuf = currBuf
            break
        }
        val indie = buffers.indexOf(targetBuf)
        println(indie)
        println(offset)
        val chunkBufferAddress = ChunkBufferAddress(indie, offset)

        val chunkAddressVector = (chunkPos + chunkAddressOffset).mod(renderDistance * 2 + 1)
        val chunkAddressIndex = (chunkAddressVector.z * renderDistance.z * renderDistance.z) +
                (chunkAddressVector.y * renderDistance.y) +
                (chunkAddressVector.x)
        val compressed = chunkBufferAddress.compress()
        addressBuffer.putInt(chunkAddressIndex, compressed)
        println(compressed)

        return chunkBufferAddress
    }

    private fun createNewBuffer(): ChunkBuffer {
        val bufLayout = VulkanBufferLayout(
            BUFFER_SIZE.toLong(),
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.STORAGE_BUFFER
        )
        bufferArrayChanged = true
        val buf = ChunkBuffer(buffers.size, bufferFactory.createBuffer(bufLayout), BUFFER_SIZE)
        buffers.add(buf)
        return buf
    }


    fun <R> mapBuffers(block: (ChunkBuffer) -> R): List<R> {
        return buffers.map(block)
    }


    fun destroy() {
        buffers.forEach { it.destroy() }
        addressBuffer.destroy()
    }
}