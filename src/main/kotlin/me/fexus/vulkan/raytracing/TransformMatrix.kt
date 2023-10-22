package me.fexus.vulkan.raytracing

import me.fexus.math.mat.Mat4
import me.fexus.math.vec.Vec4
import java.nio.ByteBuffer


// 3x4 row-major matrix
class TransformMatrix(val rows: Array<Vec4>) {
    constructor(mat4: Mat4): this(
        arrayOf(
            Vec4(mat4[0,0], mat4[1,0], mat4[2,0], mat4[3,0]),
            Vec4(mat4[0,1], mat4[1,1], mat4[2,1], mat4[3,1]),
            Vec4(mat4[0,2], mat4[1,2], mat4[2,2], mat4[3,2]),
        )
    )

    constructor(
        xx: Float, xy: Float, xz: Float, xw: Float,
        yx: Float, yy: Float, yz: Float, yw: Float,
        zx: Float, zy: Float, zz: Float, zw: Float
    ): this(
        arrayOf(
            Vec4(xx, xy, xz, xw),
            Vec4(yx, yy, yz, yw),
            Vec4(zx, zy, zz, zw)
        )
    )

    fun toByteBuffer(target: ByteBuffer, offset: Int) {
        rows[0].toByteBuffer(target, offset)
        rows[1].toByteBuffer(target, offset + Vec4.SIZE_BYTES)
        rows[2].toByteBuffer(target, offset + (Vec4.SIZE_BYTES * 2))
    }

    companion object {
        const val SIZE_BYTES = 48
    }
}