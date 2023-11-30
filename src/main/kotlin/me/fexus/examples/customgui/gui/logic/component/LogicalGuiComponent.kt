package me.fexus.examples.customgui.gui.logic.component

import me.fexus.math.vec.IVec2


interface LogicalGuiComponent {
    val parent: LogicalGuiComponent?
    val children: MutableList<LogicalGuiComponent>


    fun addComponent(component: LogicalGuiComponent) {
        children.add(component)
    }

    fun getAllChildren(): List<LogicalGuiComponent> {
        val rec = mutableListOf<LogicalGuiComponent>()
        children.forEach { rec.addAll(it.getAllChildren()) }
        return rec + this
    }

    fun getGlobalParentPosition(): IVec2 {
        val par = parent ?: return IVec2(0, 0)
        val parentGlobalsPos = par.getGlobalParentPosition()
        return if (par is SpatialComponent) parentGlobalsPos + par.localPosition.xy else parentGlobalsPos
    }
}