package me.fexus.fexgui.logic.component

import me.fexus.fexgui.logic.component.alignment.ComponentAlignments
import me.fexus.math.vec.IVec2
import me.fexus.math.vec.IVec3


data class ComponentSpatialData(var position: IVec3, var dimensions: IVec2, var alignment: ComponentAlignments)