package me.fexus.examples.coolvoxelrendering.event

import kotlin.reflect.KClass


class EventBus<R: Any> {
    private val handlers = mutableSetOf<EventHandler<out R>>()

    fun <E: R> publish(event: E) {
        val handlers = this.handlers.toList()
        handlers.forEach { it.handle(event) }
    }

    inline fun <reified E: R> on(tag: Any, noinline handler: (E) -> Unit) = this.on(E::class, tag, handler = handler)
    fun <E: R> on(type: KClass<E>, tag: Any, handler: (E) -> Unit) {
        handlers.add(EventHandler(type, tag, handler))
    }

    fun remove(tag: Any){
        handlers.removeIf { tag == it.tag }
    }
}