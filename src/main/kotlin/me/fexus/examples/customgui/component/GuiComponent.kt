package me.fexus.examples.customgui.component

interface GuiComponent {
    val children: MutableList<GuiComponent>


    fun addComponent(component: GuiComponent) {
        children.add(component)
    }

    fun getAllChildren(): List<GuiComponent> {
        val rec = mutableListOf<GuiComponent>()
        children.forEach { rec.addAll(it.getAllChildren()) }
        return rec + this
    }
}