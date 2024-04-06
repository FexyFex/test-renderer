package me.fexus.examples.coolvoxelrendering.entity

import me.fexus.examples.coolvoxelrendering.event.EventPoster
import me.fexus.examples.coolvoxelrendering.event.EventSubscriber
import me.fexus.examples.coolvoxelrendering.util.containsInstanceOf
import me.fexus.examples.coolvoxelrendering.util.firstInstanceOfOrNull
import me.fexus.examples.coolvoxelrendering.world.position.ChunkLocalPosition
import me.fexus.examples.coolvoxelrendering.world.position.ChunkPosition
import me.fexus.examples.coolvoxelrendering.world.position.WorldPosition
import kotlin.reflect.full.isSubclassOf


abstract class Entity(val id: Long): EventPoster {
    constructor(id: Long?): this(id ?: generateEntityID())


    abstract val worldPosition: WorldPosition
    val chunkPosition: ChunkPosition; get() = worldPosition.toChunkPosition()
    val chunkLocalPosition: ChunkLocalPosition; get() = worldPosition.toChunkLocalPositon()

    val components = mutableListOf<EntityComponent>()
    val traits = mutableListOf<EntityTrait>()

    override val subscribers = mutableListOf<EventSubscriber>()


    abstract fun tick(delta: Float)


    // ---Trait functions---
    inline fun <reified T: EntityTrait> addTrait() {
        val newTraitClass = T::class
        val newTrait = newTraitClass.constructors.first().call(this)
        for ((index, trait) in traits.withIndex()) {
            val traitClass = trait::class
            // Override traits of the same or parent class
            if (newTraitClass.isSubclassOf(traitClass) || newTraitClass == traitClass) {
                traits[index] = newTrait
                return
            }
        }
        traits.add(newTrait)
    }

    inline fun <reified T: EntityTrait> hasTrait(): Boolean = traits.containsInstanceOf<T>()

    inline fun <reified T: EntityTrait> getTrait(): T? = traits.firstInstanceOfOrNull<T>()

    inline fun <reified T: EntityTrait> withTrait(action: T.() -> Unit) {
        val targetTrait = traits.firstInstanceOfOrNull<T>() ?: return
        targetTrait.action()
    }
    // ---Trait functions---


    // ---Component functions---
    fun <T: EntityComponent> addComponent(newComponent: T) {
        val newComponentClass = newComponent::class
        for ((index, component) in components.withIndex()) {
            val componentClass = component::class
            // Override components of the same or parent class
            if (newComponentClass.isSubclassOf(componentClass) || newComponentClass == componentClass) {
                components[index] = newComponent
                return
            }
        }
        components.add(newComponent)
    }

    inline fun <reified C: EntityComponent> hasComponent(): Boolean = components.containsInstanceOf<C>()

    inline fun <reified C: EntityComponent> getComponent(): C? = components.firstInstanceOfOrNull<C>()

    inline fun <reified C: EntityComponent> withComponent(action: C.() -> Unit) {
        val targetComponent = components.firstInstanceOfOrNull<C>() ?: return
        targetComponent.action()
    }
    // ---Component functions---


    companion object {
        private var nextID: Long = 1L
        fun generateEntityID(): Long {
            return nextID++
        }
    }
}