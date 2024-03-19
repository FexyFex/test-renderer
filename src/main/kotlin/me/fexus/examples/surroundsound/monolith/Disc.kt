package me.fexus.examples.surroundsound.monolith

import de.javagl.jgltf.model.io.GltfModelReader
import java.nio.ByteBuffer


class Disc {
    data class Model(
        val byteBuffer: ByteBuffer,
        val vertexOffset: Int, val indexOffset: Int, val indexCount: Int,
        val posOffset: Int, val uvOffset: Int, val normalOffset: Int,
    )

    companion object {
        fun loadModel(): Disc.Model {
            val uri = ClassLoader.getSystemResource("models/disc.glb").toURI()
            val modelReader = GltfModelReader()
            val model = modelReader.read(uri)

            val byteBuffer = model.bufferModels.first().bufferData

            val scene = model.sceneModels.first()

            val disk = scene.nodeModels[0].meshModels.first().meshPrimitiveModels.first()

            val offset = disk.attributes["POSITION"]!!.bufferViewModel.byteOffset

            return Model(
                byteBuffer,
                offset,
                disk.indices.bufferViewModel.byteOffset,
                disk.indices.count,
                offset,
                disk.attributes["TEXCOORD_0"]!!.bufferViewModel.byteOffset - offset,
                disk.attributes["NORMAL"]!!.bufferViewModel.byteOffset - offset
            )
        }
    }
}