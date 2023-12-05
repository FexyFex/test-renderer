package me.fexus.fexgui

import me.fexus.fexgui.graphic.component.GraphicalUIComponent
import me.fexus.fexgui.logic.component.SpatialComponent
import me.fexus.fexgui.util.put
import java.nio.ByteBuffer


data class AssembledComponent(val logicComponent: SpatialComponent, val graphicsComponent: GraphicalUIComponent) {
    fun writePushConstantsBuffer(buffer: ByteBuffer) {
        val absPos = logicComponent.globalPosition
        buffer.put(0, absPos)
        buffer.put(8, logicComponent.spatialData.dimensions)
        buffer.putInt(16, absPos.z)
        graphicsComponent.writePushConstantsBuffer(buffer, 20)
    }
}