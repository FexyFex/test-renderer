package me.fexus.fexgui.logic.component

import me.fexus.fexgui.logic.component.visualRequirement.ComponentVisualRequirement
import me.fexus.math.vec.IVec3


abstract class SpatialComponent(var spatialData: ComponentSpatialData): LogicalUIComponent {
    abstract val visualRequirements: ComponentVisualRequirement
    override val children = mutableListOf<LogicalUIComponent>()
    override var destroyed: Boolean = false

    val globalPosition: IVec3; get() = IVec3(getGlobalParentPosition(), 0) + spatialData.position
}