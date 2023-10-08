package me.fexus.texture

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryUtil
import java.lang.NullPointerException
import java.nio.ByteBuffer


class TextureLoader(imagePath: String) {
    private val texWidth = IntArray(1) { 0 }
    private val texHeight = IntArray(1) { 0 }
    var pixels: ByteBuffer; private set

    val width: Int; get() = texWidth[0]
    val height: Int; get() = texHeight[0]
    val imageSize: Long; get() = (width * height * 4).toLong()


    init {
        val texChannels = IntArray(1) { 0 }

        runMemorySafe {
            val imageBytes = ClassLoader.getSystemResource(imagePath).readBytes()
            val pImage = allocate(imageBytes.size)
            imageBytes.forEachIndexed { index, byte -> pImage.put(index, byte) }

            pixels = STBImage.stbi_load_from_memory(pImage, texWidth, texHeight, texChannels, STBImage.STBI_rgb_alpha)
                ?: throw Exception("File is not an image: $imagePath")
        }
    }


    fun freeImage(){
        STBImage.stbi_image_free(pixels)
    }
}