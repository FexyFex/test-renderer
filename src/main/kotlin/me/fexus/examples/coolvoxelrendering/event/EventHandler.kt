package me.fexus.examples.coolvoxelrendering.event

import kotlin.reflect.KClass


internal class EventHandler<E : Any>(val type: KClass<E>, val tag: Any, val handler: (E) -> Unit) {
    fun handle(event: Any): Boolean {
        if (type.java.isInstance(event)) {
            @Suppress("UNCHECKED_CAST")
            handler(event as? E ?: return false)
            return true
        }
        return false
    }
}