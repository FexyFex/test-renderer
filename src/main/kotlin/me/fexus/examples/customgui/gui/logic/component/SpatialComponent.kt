package me.fexus.examples.customgui.gui.logic.component

import me.fexus.examples.customgui.gui.logic.component.alignment.ComponentAlignments
import me.fexus.math.vec.IVec2
import me.fexus.math.vec.IVec3


abstract class SpatialComponent: LogicalGuiComponent {
    override val children = mutableListOf<LogicalGuiComponent>()
    abstract var localPosition: IVec3
    abstract var extent: IVec2
    abstract var alignment: ComponentAlignments

    val globalPosition: IVec3; get() = IVec3(getGlobalParentPosition() + localPosition.xy, localPosition.z)
}