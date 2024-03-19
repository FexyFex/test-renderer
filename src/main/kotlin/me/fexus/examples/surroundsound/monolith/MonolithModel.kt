package me.fexus.examples.surroundsound.monolith

import de.javagl.jgltf.model.GltfModel
import de.javagl.jgltf.model.io.GltfModelReader
import java.nio.ByteBuffer


data class MonolithModel(val buffer: ByteBuffer, val outerMesh: SubMesh, val innerMesh: SubMesh, val model: GltfModel) {

    data class SubMesh(
        val firstVertex: Int, val indexOffset: Int, val indexCount: Int,
        val posOffset: Int, val uvOffset: Int, val normalOffset: Int,
    )

    companion object {
        fun load(): MonolithModel {
            val uri = ClassLoader.getSystemResource("models/monolith.glb").toURI()
            val modelReader = GltfModelReader()
            val model = modelReader.read(uri)

            val byteBuffer = model.bufferModels.first().bufferData

            val scene = model.sceneModels.first()

            val outerData = scene.nodeModels[0].meshModels.first().meshPrimitiveModels.first()
            val innerData = scene.nodeModels[1].meshModels.first().meshPrimitiveModels.first()

            val outerOffset = outerData.attributes["POSITION"]!!.bufferViewModel.byteOffset
            val outerMesh = SubMesh(
                outerOffset,
                outerData.indices.bufferViewModel.byteOffset,
                outerData.indices.count,
                outerOffset,
                outerData.attributes["TEXCOORD_0"]!!.bufferViewModel.byteOffset - outerOffset,
                outerData.attributes["NORMAL"]!!.bufferViewModel.byteOffset - outerOffset
            )

            val innerOffset = innerData.attributes["POSITION"]!!.bufferViewModel.byteOffset
            val innerMesh = SubMesh(
                innerOffset,
                innerData.indices.bufferViewModel.byteOffset,
                innerData.indices.count,
                innerOffset,
                innerData.attributes["TEXCOORD_0"]!!.bufferViewModel.byteOffset - innerOffset,
                innerData.attributes["NORMAL"]!!.bufferViewModel.byteOffset - innerOffset
            )

            return MonolithModel(byteBuffer, outerMesh, innerMesh, model)
        }
    }
}