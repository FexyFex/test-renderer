package me.fexus.examples.customgui.gui.logic

import me.fexus.examples.customgui.gui.logic.component.LogicalGuiComponent
import me.fexus.examples.customgui.gui.logic.component.PhantomComponent


class LogicalGUI {
    private val root = PhantomComponent(null)


    fun addComponent(component: LogicalGuiComponent) = root.addComponent(component)

    fun getAllComponents() = root.getAllChildren()
}