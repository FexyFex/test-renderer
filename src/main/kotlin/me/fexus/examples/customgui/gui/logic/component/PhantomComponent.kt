package me.fexus.examples.customgui.gui.logic.component

class PhantomComponent(override val parent: LogicalGuiComponent?): LogicalGuiComponent {
    override val children = mutableListOf<LogicalGuiComponent>()
}