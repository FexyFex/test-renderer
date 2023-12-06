package me.fexus.fexgui.logic.component

import me.fexus.fexgui.logic.component.visual.SubComponent
import me.fexus.fexgui.logic.component.visual.SubComponentConfiguration
import me.fexus.fexgui.logic.component.visual.VisualLayout
import me.fexus.math.vec.Vec4
import kotlin.math.roundToInt


class ColorRect(
    override val parent: LogicalUIComponent?,
    spatialData: ComponentSpatialData,
    val color: Vec4,
): SpatialComponent(spatialData) {
    override val visualLayout: VisualLayout = VisualLayout(
        listOf(
            SubComponent { SubComponentConfiguration(this.spatialData, null, 1, colorToInt()) }
        )
    )

    private fun colorToInt(): Int {
        val r = (color.r * 255).roundToInt()
        val g = (color.g * 255).roundToInt()
        val b = (color.b * 255).roundToInt()
        val a = (color.a * 255).roundToInt()

        return (r) or (g shl 8) or (b shl 16) or (a shl 24)
    }
}