package me.fexus.vulkan.raytracing.accelerationstructure

import me.fexus.vulkan.raytracing.TransformMatrix
import java.nio.ByteBuffer


class AccelerationStructureInstance(
    val transform: TransformMatrix,
    val instanceCustomIndex: Int,
    val mask: Int,
    val instanceShaderBindingTableRecordOffset: Int,
    val flags: Int,
    val accelerationStructureReference: Long
) {

    fun toByteBuffer(target: ByteBuffer, offset: Int) {
        val int1 = instanceCustomIndex or (mask shl 24)
        val int2 = instanceShaderBindingTableRecordOffset or (flags shl 24)

        transform.toByteBuffer(target, offset)
        target.putInt(offset + TransformMatrix.SIZE_BYTES, int1)
        target.putInt(offset + TransformMatrix.SIZE_BYTES + Int.SIZE_BYTES, int2)
        target.putLong(offset + TransformMatrix.SIZE_BYTES + (Int.SIZE_BYTES * 2), accelerationStructureReference)
    }

    companion object {
        const val SIZE_BYTES = TransformMatrix.SIZE_BYTES + 4 + 4 + 8 // 64 Bytes Total
    }
}