package me.fexus.fexgui.logic.component.visual.flag

interface VisualFlags {
    val bits: Int

    operator fun plus(other: VisualFlags) = CombinedVisualFlags(this.bits or other.bits)

    operator fun contains(other: VisualFlags) = (this.bits and other.bits) == other.bits
}