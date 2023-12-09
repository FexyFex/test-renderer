package me.fexus.fexgui.logic.component.visual

import me.fexus.fexgui.logic.component.ComponentSpatialData
import me.fexus.fexgui.logic.component.visual.flag.VisualFlags
import me.fexus.fexgui.textureresource.TextureResource
import me.fexus.fexgui.util.Color


data class SubComponentConfiguration(
    val spatialData: ComponentSpatialData,
    val baseColor: Color,
    val texture: TextureResource?,
    val visualFlags: VisualFlags
)