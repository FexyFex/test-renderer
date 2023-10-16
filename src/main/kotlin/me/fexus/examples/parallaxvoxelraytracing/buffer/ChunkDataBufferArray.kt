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
    private var chunkAddressOffset: IVec3 = IVec3(0,0,0)


    fun init(bufferFactory: VulkanBufferFactory, renderDistance: IVec3) {
        this.bufferFactory = bufferFactory
        val addressBufferLayout = VulkanBufferLayout(
            renderDistance.x * 2 + renderDistance.y + renderDistance.z.toLong(),
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.STORAGE_BUFFER
        )
        this.addressBuffer = bufferFactory.createBuffer(addressBufferLayout)
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
                break
            }
            val currBuf = buffers[index++]
            val lOffset = currBuf.allocate(data) ?: continue
            offset = lOffset
            targetBuf = currBuf
            break
        }
        val chunkBufferAddress = ChunkBufferAddress(buffers.indexOf(targetBuf), offset)

        val chunkAddressVector = (chunkPos + chunkAddressOffset).mod(renderDistance)
        val chunkAddressIndex =
            (chunkAddressVector.z * renderDistance.z * renderDistance.z) +
                    (chunkAddressVector.y * renderDistance.y) +
                    (chunkAddressVector.x)
        addressBuffer.putInt(chunkAddressIndex, chunkBufferAddress.compress())

        return chunkBufferAddress
    }

    private fun createNewBuffer(): ChunkBuffer {
        val bufLayout = VulkanBufferLayout(
            BUFFER_SIZE.toLong(),
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.STORAGE_BUFFER
        )
        bufferArrayChanged = true
        return ChunkBuffer(buffers.size, bufferFactory.createBuffer(bufLayout), BUFFER_SIZE)
    }


    fun <R> mapBuffers(block: (ChunkBuffer) -> R): List<R> {
        return buffers.map(block)
    }


    fun destroy() {
        buffers.forEach { it.destroy() }
    }
}