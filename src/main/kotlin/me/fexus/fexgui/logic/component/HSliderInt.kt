package me.fexus.fexgui.logic.component

import me.fexus.fexgui.logic.component.visual.SubComponent
import me.fexus.fexgui.logic.component.visual.SubComponentConfiguration
import me.fexus.fexgui.logic.component.visual.VisualLayout
import me.fexus.fexgui.logic.component.visual.flag.VisualFlag
import me.fexus.fexgui.textureresource.TextureResource
import me.fexus.fexgui.util.Color
import me.fexus.math.vec.IVec2
import me.fexus.math.vec.IVec3
import kotlin.math.min
import kotlin.math.roundToInt


class HSliderInt(
    override val parent: LogicalUIComponent?,
    spatialData: ComponentSpatialData,
    val min: Int,
    val max: Int,
    val backgroundTexture: TextureResource,
    val barTexture: TextureResource,
    val sliderTexture: TextureResource,
    initialValue: Int = min
): SpatialComponent(spatialData) {
    var currentValue: Int = initialValue

    override val visualLayout = VisualLayout(
        listOf(
            SubComponent { SubComponentConfiguration(this.spatialData, Color.INVISIBLE, backgroundTexture, VisualFlag.NONE) },
            SubComponent { SubComponentConfiguration(calculateBarSpatial(), Color.INVISIBLE, barTexture, VisualFlag.NONE) },
            SubComponent { SubComponentConfiguration(calculateSliderSpatial(), Color.INVISIBLE, sliderTexture, VisualFlag.NONE) }
        )
    )


    private fun calculateSliderSpatial(): ComponentSpatialData {
        val progress: Float = currentValue.toFloat() / (max - min)
        val startPos = spatialData.position.xy + ((spatialData.dimensions * (1f - SLIDER_RELATIVE_SIZE)) / 2f)
        val endPos = IVec2(spatialData.position.x + spatialData.dimensions.x - startPos.x, startPos.y)
        val dimX = (this.spatialData.dimensions.x * 0.05f).roundToInt()
        val dimY = min((spatialData.dimensions.y * SLIDER_RELATIVE_SIZE).roundToInt(), sliderTexture.height)
        val dimensions = IVec2(dimX, dimY)
        val actualPos = startPos + ((endPos - startPos) * progress)
        val z = spatialData.position.z + 2
        return ComponentSpatialData(IVec3(actualPos, z), dimensions, this.spatialData.alignment)
    }

    private fun calculateBarSpatial(): ComponentSpatialData {
        return this.spatialData
    }


    companion object {
        const val BAR_RELATIVE_SIZE: Float = 0.95f
        const val SLIDER_RELATIVE_SIZE: Float = 0.88f
    }
}