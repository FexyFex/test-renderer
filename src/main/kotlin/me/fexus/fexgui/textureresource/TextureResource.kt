package me.fexus.fexgui.textureresource

import me.fexus.texture.TextureLoader
import java.nio.ByteBuffer


sealed class TextureResource(texturePath: String) {
    abstract val name: String
    private val loader = TextureLoader(texturePath)

    val width: Int; get() = loader.width
    val height: Int; get() = loader.height
    val bufferSize: Long; get() = loader.imageSize
    val pixelBuffer: ByteBuffer; get() = loader.pixels
}