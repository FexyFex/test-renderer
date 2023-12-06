package me.fexus.fexgui.logic.component.visual

import me.fexus.fexgui.logic.component.ComponentSpatialData
import me.fexus.fexgui.textureresource.TextureResource


data class SubComponentConfiguration(
    val spatialData: ComponentSpatialData,
    val texture: TextureResource?,
    val typeFlags: Int,
    val dataFlags: Int,
)