package me.fexus.examples.customgui.gui.logic.component.alignment

interface ComponentAlignments {
    val bits: Int

    operator fun plus(other: ComponentAlignments): ComponentAlignments {
        return CombinedComponentAlignment(this.bits or other.bits)
    }


    operator fun contains(other: ComponentAlignments): Boolean {
        return (this.bits and other.bits) == other.bits
    }
}