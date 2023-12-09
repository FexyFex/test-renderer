package me.fexus.fexgui.logic.component.visual.flag

enum class VisualFlag(override val bits: Int): VisualFlags {
    NONE(0),
    UNTEXTURED(1),
    BLANK_IMAGE(2)
}