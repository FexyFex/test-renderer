package me.fexus.fexgui.logic.component

import me.fexus.fexgui.logic.component.visualRequirement.VisualRequirementOneColor
import me.fexus.math.vec.Vec4


class ColorRect(
    override val parent: LogicalUIComponent?,
    spatialData: ComponentSpatialData,
    val color: Vec4,
): SpatialComponent(spatialData) {
    override val visualRequirements = VisualRequirementOneColor(color)
}