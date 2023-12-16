package me.fexus.fexgui.logic.component.visual.flag

enum class VisualFlag(override val bits: Int): VisualFlags {
    NONE(0),
    TEXTURED(1),
    TEXT_IMAGE(2)
}