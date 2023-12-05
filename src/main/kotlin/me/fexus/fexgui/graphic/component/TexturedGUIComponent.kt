package me.fexus.fexgui.graphic.component

import me.fexus.fexgui.graphic.vulkan.IndexedVulkanImage
import java.nio.ByteBuffer


class TexturedGUIComponent(val indexedVulkanImage: IndexedVulkanImage): GraphicalUIComponent {
    override fun writePushConstantsBuffer(buffer: ByteBuffer, offset: Int) {
        buffer.putInt(offset, indexedVulkanImage.index)
    }
}