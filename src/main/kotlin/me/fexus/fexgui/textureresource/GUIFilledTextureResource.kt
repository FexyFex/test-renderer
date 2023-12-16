package me.fexus.fexgui.textureresource

import me.fexus.texture.TextureLoader
import java.nio.ByteBuffer


class GUIFilledTextureResource(override val name: String, texturePath: String): GUITextureResource {
    private val loader = TextureLoader(texturePath)

    override val width: Int; get() = loader.width
    override val height: Int; get() = loader.height
    val bufferSize: Long; get() = loader.imageSize
    val pixelBuffer: ByteBuffer; get() = loader.pixels
}