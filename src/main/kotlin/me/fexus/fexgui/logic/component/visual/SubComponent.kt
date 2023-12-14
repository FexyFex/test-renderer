package me.fexus.fexgui.logic.component.visual

import me.fexus.fexgui.logic.component.ComponentSpatialData
import me.fexus.fexgui.logic.component.visual.flag.VisualFlags
import me.fexus.fexgui.textureresource.GUIFilledTextureResource
import me.fexus.fexgui.textureresource.GUITextureResource
import me.fexus.fexgui.util.Color


class SubComponent(
    val baseColor: Color,
    val textureResource: GUITextureResource?,
    val visualFlags: VisualFlags,
    val getSpatialData: () -> ComponentSpatialData,
)