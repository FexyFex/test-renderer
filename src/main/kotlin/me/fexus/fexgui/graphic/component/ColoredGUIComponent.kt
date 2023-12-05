package me.fexus.fexgui.graphic.component

import me.fexus.math.vec.Vec4
import java.nio.ByteBuffer


class ColoredGUIComponent(val color: Vec4): GraphicalUIComponent {
    override fun writePushConstantsBuffer(buffer: ByteBuffer, offset: Int) {
        color.toByteBuffer(buffer, offset)
    }
}