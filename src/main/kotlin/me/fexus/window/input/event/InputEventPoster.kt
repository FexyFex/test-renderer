package me.fexus.window.input.event

import me.fexus.math.vec.IVec2
import me.fexus.window.input.Key
import me.fexus.window.input.MouseButton


abstract class InputEventPoster {
    private val subscribers = mutableListOf<InputEventSubscriber>()


    fun registerSubscriber(newSub: InputEventSubscriber) = subscribers.add(newSub)
    fun unregisterSubScriber(sub: InputEventSubscriber) = subscribers.remove(sub)

    fun postKeyPressed(key: Key) {
        subscribers.forEach { it.onKeyPressed(key) }
    }

    fun postKeyReleased(key: Key) {
        subscribers.forEach { it.onKeyReleased(key) }
    }

    fun postCharTyped(char: Char) {
        subscribers.forEach { it.onCharTyped(char) }
    }

    fun postMouseMoved(newPos: IVec2) {
        subscribers.forEach { it.onMouseMoved(newPos) }
    }

    fun postMouseButtonPressed(button: MouseButton) {
        subscribers.forEach { it.onMouseButtonPressed(button) }
    }

    fun postMouseButtonReleased(button: MouseButton) {
        subscribers.forEach { it.onMouseButtonReleased(button) }
    }
}