package me.fexus.fexgui.graphic.component

import java.nio.ByteBuffer


interface GraphicalUIComponent {
    fun writePushConstantsBuffer(buffer: ByteBuffer, offset: Int)
}