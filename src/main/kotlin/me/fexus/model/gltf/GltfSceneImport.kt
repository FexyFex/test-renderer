package me.fexus.model.gltf

import de.javagl.jgltf.model.io.GltfModelReader

class GltfSceneImport {


    fun import(pathInResources: String) {
        val uri = ClassLoader.getSystemResource(pathInResources).toURI()
        val modelReader = GltfModelReader()
        val model = modelReader.read(uri)

        val scenes = model.sceneModels
    }
}