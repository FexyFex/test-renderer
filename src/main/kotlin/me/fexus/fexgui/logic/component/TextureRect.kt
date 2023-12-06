package me.fexus.fexgui.logic.component

import me.fexus.fexgui.logic.component.visual.SubComponent
import me.fexus.fexgui.logic.component.visual.SubComponentConfiguration
import me.fexus.fexgui.logic.component.visual.VisualLayout
import me.fexus.fexgui.textureresource.TextureResource


class TextureRect(
    override val parent: LogicalUIComponent?,
    spatialData: ComponentSpatialData,
    val textureResource: TextureResource
): SpatialComponent(spatialData) {
    override val visualLayout: VisualLayout = VisualLayout(
        listOf(
            SubComponent { SubComponentConfiguration(this.spatialData, textureResource, 0, 0) }
        )
    )
}