package me.fexus.examples.customgui.component

import me.fexus.examples.customgui.component.alignment.ComponentAlignments
import me.fexus.math.vec.IVec2
import me.fexus.math.vec.IVec3


abstract class SpatialComponent: GuiComponent {
    override val children = mutableListOf<GuiComponent>()
    abstract var position: IVec3
    abstract var extent: IVec2
    abstract var alignment: ComponentAlignments
}