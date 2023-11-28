package me.fexus.voxel.octree.buffer

import java.nio.ByteBuffer
import java.nio.ByteOrder


// Reference: https://github.com/Kotlin/kotlinx-io/blob/master/bytestring/common/src/ByteStringBuilder.kt (& epic Fxshlein code)
class SVOBufferBuilder {
    private val svoNodes = mutableListOf<UnsafeSVONode>()
    private val markers = mutableMapOf<Int, UnsafeBufferReference>()
    private var offset = 0


    fun toByteBuffer(): ByteBuffer {
        val buffer = ByteBuffer.allocate(offset)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        toByteBuffer(buffer, 0)
        return buffer
    }

    fun toByteBuffer(buffer: ByteBuffer, offset: Int) {
        svoNodes.forEach {
            var lOffset = it.offset + offset
            buffer.putInt(lOffset, it.childCount) // could be 16 bits
            lOffset += Int.SIZE_BYTES

            it.childPointers.forEach { childPointer ->
                assert(childPointer.reference.value > 0)

                buffer.putInt(lOffset, childPointer.octantIndex) // could be 3 or 4 bits
                lOffset += Int.SIZE_BYTES
                buffer.putInt(lOffset, childPointer.reference.value) // could be 16 bits
                lOffset += Int.SIZE_BYTES
            }
        }
    }


    fun append(node: IndexedOctreeNode) {
        val size = ((node.children.size) * Int.SIZE_BYTES * 2) + Int.SIZE_BYTES

        mark(node.index, offset)

        val childPointers = node.children.map {
            UnsafeSVOChildPointer(it.octantIndex, reference(it.child.index))
        }
        val unresolvedDAGNode = UnsafeSVONode(this.offset, childPointers.size, childPointers)
        svoNodes.add(unresolvedDAGNode)

        this.offset += size
    }

    private fun reference(index: Int): UnsafeBufferReference {
        val ref = markers[index]
        return if (ref == null) {
            val newRef = UnsafeBufferReference()
            markers[index] = newRef
            newRef
        } else {
            ref.value = offset
            ref
        }
    }

    private fun mark(index: Int, offset: Int) {
        markers.getOrPut(index) { UnsafeBufferReference() }.value = offset
    }


    private data class UnsafeSVONode(val offset: Int, val childCount: Int, val childPointers: List<UnsafeSVOChildPointer>)
    private data class UnsafeSVOChildPointer(val octantIndex: Int, val reference: UnsafeBufferReference)
    private class UnsafeBufferReference {
        var value: Int = -1
            set(newValue) {
                if (field == -1) field = newValue
            }

        override fun toString(): String {
            return "UnsafeBufferReference($value)"
        }
    }
}

inline fun buildSVOBuffer(buildBlock: SVOBufferBuilder.() -> Unit): ByteBuffer {
    return SVOBufferBuilder().apply(buildBlock).toByteBuffer()
}