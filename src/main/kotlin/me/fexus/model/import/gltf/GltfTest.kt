package me.fexus.model.import.gltf


fun main() {
    val decoder = GlTFDecoder()

    val fileData = ClassLoader.getSystemResource("models/monolith.glb").readBytes()
    val gltf = decoder.read(fileData)

    gltf.meshModels

    val buffers = gltf.bufferModels.map { it.bufferData }
    val models = gltf.sceneModels.first().nodeModels.map { it.meshModels }
}