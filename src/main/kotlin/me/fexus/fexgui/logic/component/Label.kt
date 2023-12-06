package me.fexus.fexgui.logic.component

import me.fexus.fexgui.logic.component.visual.SubComponent
import me.fexus.fexgui.logic.component.visual.SubComponentConfiguration
import me.fexus.fexgui.logic.component.visual.VisualLayout


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
            SubComponent { SubComponentConfiguration(this.spatialData, null, 2, 0) }
        )
    )
}