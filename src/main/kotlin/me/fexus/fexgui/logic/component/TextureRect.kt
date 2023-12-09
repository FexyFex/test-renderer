package me.fexus.fexgui.logic.component

import me.fexus.fexgui.logic.component.visual.SubComponent
import me.fexus.fexgui.logic.component.visual.SubComponentConfiguration
import me.fexus.fexgui.logic.component.visual.VisualLayout
import me.fexus.fexgui.logic.component.visual.flag.VisualFlag
import me.fexus.fexgui.textureresource.TextureResource
import me.fexus.fexgui.util.Color


class TextureRect(
    override val parent: LogicalUIComponent?,
    spatialData: ComponentSpatialData,
    val textureResource: TextureResource
): SpatialComponent(spatialData) {
    override val visualLayout: VisualLayout = VisualLayout(
        listOf(
            SubComponent { SubComponentConfiguration(this.spatialData, Color.INVISIBLE, textureResource, VisualFlag.NONE) }
        )
    )
}