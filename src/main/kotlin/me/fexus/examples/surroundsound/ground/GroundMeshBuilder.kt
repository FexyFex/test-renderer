package me.fexus.examples.surroundsound.ground

import me.fexus.examples.surroundsound.Mesh
import me.fexus.math.vec.Vec3
import java.nio.ByteBuffer
import java.nio.ByteOrder


class GroundMeshBuilder {
    companion object {
        private const val VERTEX_SIZE = 16
    }

    fun createNewGroundMesh(heightmap: GroundHeightmap): Mesh {
        val faceCount = (heightmap.width - 1) * (heightmap.breadth - 1)
        val vertexBuffer = ByteBuffer.allocate(faceCount * 4 * VERTEX_SIZE) // 4 vertices per quad
        val indexBuffer = ByteBuffer.allocate(faceCount * 6 * 4) // 6 indices per quad, 4 bytes per index
        vertexBuffer.order(ByteOrder.LITTLE_ENDIAN)
        indexBuffer.order(ByteOrder.LITTLE_ENDIAN)

        var vertexOffset = 0
        var vertexIndex = 0
        var indexOffset = 0

        fun writeFace(vararg verts: Vec3) {
            verts.forEach { vert ->
                (vert).intoByteBuffer(vertexBuffer, vertexOffset)
                vertexBuffer.putFloat(vertexOffset + 12, 1.0f)
                vertexOffset += VERTEX_SIZE
            }

            indexBuffer.putInt(indexOffset, vertexIndex++)
            indexOffset += Int.SIZE_BYTES
            indexBuffer.putInt(indexOffset, vertexIndex++)
            indexOffset += Int.SIZE_BYTES
            indexBuffer.putInt(indexOffset, vertexIndex)
            indexOffset += Int.SIZE_BYTES
            indexBuffer.putInt(indexOffset, vertexIndex++)
            indexOffset += Int.SIZE_BYTES
            indexBuffer.putInt(indexOffset, vertexIndex)
            indexOffset += Int.SIZE_BYTES
            indexBuffer.putInt(indexOffset, vertexIndex++ - 2)
            indexOffset += Int.SIZE_BYTES
        }

        for (x in 0 until heightmap.width - 1) {
            for (z in 0 until heightmap.breadth - 1) {
                val current = heightmap[x, z]
                val nextX = heightmap[x + 1, z]
                val nextZ = heightmap[x, z + 1]
                val nextXZ = heightmap[x + 1, z + 1]

                val vert1 = Vec3(x, current, z)
                val vert2 = Vec3(x + 1, nextX, z)
                val vert3 = Vec3(x, nextZ, z + 1)
                val vert4 = Vec3(x + 1, nextXZ, z + 1)

                writeFace(vert1, vert2, vert3, vert4)
            }
        }

        return Mesh(vertexBuffer, indexBuffer)
    }
}