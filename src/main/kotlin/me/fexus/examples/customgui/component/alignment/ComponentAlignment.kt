package me.fexus.examples.customgui.component.alignment

enum class ComponentAlignment(override val bits: Int): ComponentAlignments {
    LEFT(1),
    RIGHT(2),
    CENTERED(4),
    TOP(8),
    BOTTOM(16)
}