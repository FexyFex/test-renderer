package me.fexus.fexgui.graphic

import me.fexus.fexgui.graphic.vulkan.IndexedVulkanImage
import me.fexus.fexgui.logic.component.SpatialComponent
import me.fexus.fexgui.util.Color
import me.fexus.fexgui.util.put
import me.fexus.memory.runMemorySafe


data class ComponentRenderer(val logicComponent: SpatialComponent, val images: List<IndexedVulkanImage?>) {
    fun render(cmdContext: GraphicalUIVulkan.CommandBufferContext) = runMemorySafe {
        val pushConsts = allocate(128)

        logicComponent.visualLayout.subComponents.forEachIndexed { index, it ->
            val spatialData = it.getSpatialData()
            val position = spatialData.position

            pushConsts.put(0, spatialData.position)
            pushConsts.put(8, spatialData.dimensions)
            pushConsts.putInt(16, position.z)
            pushConsts.putInt(20, spatialData.alignment.bits)
            pushConsts.putInt(24, images[index]?.index ?: -1)
            pushConsts.putInt(28, it.baseColor.toInt(Color.Format.RGBA8))

            cmdContext.pushConstants(pushConsts)
            cmdContext.drawIndexed()
        }
    }
}