package me.fexus.fexgui.logic.component

import me.fexus.fexgui.logic.component.visual.SubComponent
import me.fexus.fexgui.logic.component.visual.VisualLayout
import me.fexus.fexgui.logic.component.visual.flag.VisualFlag
import me.fexus.fexgui.textureresource.GUIEmptyTextureResource
import me.fexus.fexgui.util.Color


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

    override val visualLayout: VisualLayout = VisualLayout(
        listOf(
            // TODO: create a fitting texture resource
            SubComponent(
                Color.INVISIBLE,
                GUIEmptyTextureResource("label", spatialData.dimensions.x, spatialData.dimensions.y),
                VisualFlag.TEXT_IMAGE,
                this::spatialData
            )
        )
    )
}