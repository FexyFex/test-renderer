package me.fexus.fexgui

import me.fexus.fexgui.graphic.GraphicalUIVulkan
import me.fexus.fexgui.logic.component.SpatialComponent
import me.fexus.fexgui.util.put
import me.fexus.memory.runMemorySafe


data class ComponentRenderer(val logicComponent: SpatialComponent) {
    fun render(cmdContext: GraphicalUIVulkan.CommandBufferContext) = runMemorySafe {
        val pushConsts = allocate(128)

        logicComponent.visualLayout.subComponents.forEach {
            val config = it.createConfig()
            val position = config.spatialData.position

            pushConsts.put(0, config.spatialData.position)
            pushConsts.put(8, config.spatialData.dimensions)
            pushConsts.putInt(16, position.z)

            cmdContext.pushConstants(pushConsts)
            cmdContext.drawIndexed()
        }
    }
}