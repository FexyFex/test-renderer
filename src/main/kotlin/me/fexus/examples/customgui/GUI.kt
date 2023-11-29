package me.fexus.examples.customgui

import me.fexus.examples.customgui.component.GuiComponent
import me.fexus.examples.customgui.component.PhantomComponent


class GUI {
    private val root = PhantomComponent()


    fun addComponent(component: GuiComponent) = root.addComponent(component)

    fun getAllComponents() = root.getAllChildren()
}