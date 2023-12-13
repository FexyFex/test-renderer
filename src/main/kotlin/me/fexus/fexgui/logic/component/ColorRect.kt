package me.fexus.fexgui.logic.component

import me.fexus.fexgui.logic.component.visual.SubComponent
import me.fexus.fexgui.logic.component.visual.VisualLayout
import me.fexus.fexgui.logic.component.visual.flag.VisualFlag
import me.fexus.fexgui.util.Color


class ColorRect(
    override val parent: LogicalUIComponent?,
    spatialData: ComponentSpatialData,
    val color: Color,
): SpatialComponent(spatialData) {
    override val visualLayout: VisualLayout = VisualLayout(
        listOf(
            SubComponent(color, null, VisualFlag.NONE, this::spatialData)
        )
    )
}