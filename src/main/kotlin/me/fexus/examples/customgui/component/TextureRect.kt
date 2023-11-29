package me.fexus.examples.customgui.component

import me.fexus.examples.customgui.component.alignment.ComponentAlignments
import me.fexus.math.vec.IVec2
import me.fexus.math.vec.IVec3

data class TextureRect(
    override var position: IVec3,
    override var extent: IVec2,
    override var textureIndex: Int,
    override var alignment: ComponentAlignments
    ): TexturedComponent()