package me.fexus.fexgui.logic.component

import me.fexus.math.vec.IVec2


interface LogicalUIComponent {
    val parent: LogicalUIComponent?
    val children: MutableList<LogicalUIComponent>
    var destroyed: Boolean


    fun addComponent(component: LogicalUIComponent) {
        children.add(component)
    }

    fun append(createBlock: ComponentCreationContext.() -> Unit): LogicalUIComponent {
        val context = ComponentCreationContext(this)
        context.createBlock()
        context.addedComponents
            .filterIsInstance<SpatialComponent>()
            .forEach { signalComponentAdded(it) }
        return this
    }

    fun getAllChildren(): List<LogicalUIComponent> {
        val rec = mutableListOf<LogicalUIComponent>()
        children.forEach { rec.addAll(it.getAllChildren()) }
        return rec + this
    }

    fun getGlobalParentPosition(): IVec2 {
        val par = parent ?: return IVec2(0, 0)
        val parentGlobalsPos = par.getGlobalParentPosition()
        return if (par is SpatialComponent) parentGlobalsPos + par.spatialData.position.xy else parentGlobalsPos
    }

    fun destroy() {
        children.forEach(LogicalUIComponent::destroy)
        parent?.children?.remove(this)
        this.destroyed = true
    }

    fun signalComponentAdded(component: SpatialComponent) {
        parent!!.signalComponentAdded(component)
    }


    class ComponentCreationContext(private val parent: LogicalUIComponent) {
        val addedComponents = mutableListOf<LogicalUIComponent>()

        fun textField(
            spatialData: ComponentSpatialData, text: String,
            appendBlock: ComponentCreationContext.() -> Unit = {},
        ): Label {
            val component = Label(parent, spatialData, text)
            parent.addComponent(component)
            component.append(appendBlock)
            addedComponents.add(component)
            return component
        }

        fun phantom(appendBlock: ComponentCreationContext.() -> Unit = {}): PhantomComponent {
            val component = PhantomComponent(parent)
            parent.addComponent(component)
            component.append(appendBlock)
            addedComponents.add(component)
            return component
        }
    }
}