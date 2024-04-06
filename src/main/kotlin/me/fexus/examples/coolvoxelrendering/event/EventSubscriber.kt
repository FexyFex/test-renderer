package me.fexus.examples.coolvoxelrendering.event

import kotlin.reflect.KClass


interface EventSubscriber {
    val eventBus: EventBus<Any>

    fun publish(event: Any) = eventBus.publish(event)

    fun <E: Any> on(type: KClass<E>, tag: Any, handler: (E) -> Unit) = eventBus.on(type, tag, handler)
}

inline fun <R : Any, reified E : R> EventSubscriber.on(tag: Any, noinline handler: (E) -> Unit) =
    eventBus.on(E::class, tag, handler = handler)