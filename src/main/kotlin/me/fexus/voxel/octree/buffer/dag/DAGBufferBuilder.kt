package me.fexus.voxel.octree.buffer.dag

import me.fexus.voxel.octree.buffer.SVOBufferBuilder
import java.nio.ByteBuffer


// Reference: https://github.com/Kotlin/kotlinx-io/blob/master/bytestring/common/src/ByteStringBuilder.kt (& epic Fxshlein code)
class DAGBufferBuilder {
    private val unresolvedDAGNodes = mutableListOf<UnsafeDAGNode>()
    private val markers = mutableMapOf<Int, UnsafeBufferReference>()
    private var offset = 0


    fun toByteBuffer(): ByteBuffer {
        val buffer = ByteBuffer.allocate(offset)
        unresolvedDAGNodes.forEach {
            var offset = it.offset
            buffer.putInt(offset, it.childCount)
            offset += Int.SIZE_BYTES

            it.childPointers.forEach { childPointer ->
                assert(childPointer.reference.value > 0)
                buffer.putInt(offset, childPointer.octantIndex)
                offset += Int.SIZE_BYTES
                buffer.putInt(offset, childPointer.stepOffset)
                offset += Int.SIZE_BYTES
                buffer.putInt(offset, childPointer.reference.value)
                offset += Int.SIZE_BYTES
            }
        }
        return buffer
    }


    fun append(dagNode: DAGNode) {
        val size = ((dagNode.childPointers.size) * Int.SIZE_BYTES * 3) + Int.SIZE_BYTES

        mark(dagNode.index, offset)

        val childPointers = dagNode.childPointers.map {
            UnsafeDAGChildPointer(it.octantIndex, it.offset, reference(it.childIndex))
        }
        val unresolvedDAGNode = UnsafeDAGNode(this.offset, childPointers.size, childPointers)
        unresolvedDAGNodes.add(unresolvedDAGNode)

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


    private data class UnsafeDAGNode(val offset: Int, val childCount: Int, val childPointers: List<UnsafeDAGChildPointer>)
    private data class UnsafeDAGChildPointer(val octantIndex: Int, val stepOffset: Int, val reference: UnsafeBufferReference)
    private class UnsafeBufferReference {
        var value: Int = -1
            set(newValue) {
                if (this.value == -1) field = newValue
            }

    }
}

inline fun buildDAGBuffer(buildBlock: SVOBufferBuilder.() -> Unit): ByteBuffer {
    return SVOBufferBuilder().apply(buildBlock).toByteBuffer()
}