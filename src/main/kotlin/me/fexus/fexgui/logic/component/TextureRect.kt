package me.fexus.fexgui.logic.component

import me.fexus.fexgui.logic.component.visualRequirement.VisualRequirementOneTexture
import me.fexus.fexgui.textureresource.TextureResource

class TextureRect(
    override val parent: LogicalUIComponent?,
    spatialData: ComponentSpatialData,
    val textureResource: TextureResource
): SpatialComponent(spatialData) {
    override val visualRequirements = VisualRequirementOneTexture(textureResource)
}