package me.fexus.model.import.gltf

import de.javagl.jgltf.model.GltfModel
import de.javagl.jgltf.model.io.GltfAssetReader
import de.javagl.jgltf.model.io.GltfModelReader


class GlTFDecoder() {
    private val gltfAsset = GltfAssetReader()
    private val modelReader = GltfModelReader()


    fun read(gltfData: ByteArray): GltfModel {
        return modelReader.readWithoutReferences(gltfData.inputStream())
    }
}