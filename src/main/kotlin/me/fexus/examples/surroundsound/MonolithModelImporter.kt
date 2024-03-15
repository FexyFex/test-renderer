package me.fexus.examples.surroundsound

import de.javagl.jgltf.model.io.GltfModelReader
import java.nio.ByteBuffer


class MonolithModelImporter {
    var innerVertexOffset: Int = -1;

    fun loadMesh(pathInResources: String): ByteBuffer {
        val uri = ClassLoader.getSystemResource(pathInResources).toURI()
        val modelReader = GltfModelReader()
        val model = modelReader.read(uri)

        val byteBuffer = model.bufferModels.first().bufferData
        val mesh0 = model.meshModels[0].meshPrimitiveModels.first()
        val mesh1 = model.meshModels[1].meshPrimitiveModels.first()

        val bufferView = mesh1.attributes["POSITION"]!!.bufferViewModel
        this.innerVertexOffset = bufferView.byteOffset

        return byteBuffer
    }
}