package me.fexus.window.input.event

import me.fexus.math.vec.IVec2
import me.fexus.window.input.Key
import me.fexus.window.input.MouseButton


interface InputEventSubscriber {
    fun subscribe(eventPoster: InputEventPoster) {
        eventPoster.registerSubscriber(this)
    }
    fun unsubscribe(eventPoster: InputEventPoster) {
        eventPoster.unregisterSubScriber(this)
    }

    fun onKeyPressed(key: Key) {}
    fun onKeyReleased(key: Key) {}
    fun onCharTyped(char: Char) {}
    fun onMouseMoved(newPosition: IVec2) {}
    fun onMouseButtonPressed(button: MouseButton) {}
    fun onMouseButtonReleased(button: MouseButton) {}
}