package me.fexus.fexgui.logic.component

class PhantomComponent(override val parent: LogicalUIComponent?): LogicalUIComponent {
    override val children = mutableListOf<LogicalUIComponent>()
    override var destroyed: Boolean = false
}