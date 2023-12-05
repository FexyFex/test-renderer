package me.fexus.fexgui.logic.component

import me.fexus.fexgui.logic.component.visualRequirement.VisualRequirementOneEmptyTexture

class Label(
    override val parent: LogicalUIComponent?,
    spatialData: ComponentSpatialData,
    initialText: String,
): SpatialComponent(spatialData), TextComponent {
    override var text: String = initialText
        set(value) {
            field = value
            textRequiresUpdate = true
        }
    override var textRequiresUpdate: Boolean = true

    override val visualRequirements = VisualRequirementOneEmptyTexture
}