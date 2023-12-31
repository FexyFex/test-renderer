package me.fexus.window.input

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT


enum class MouseButton(val glfwValue: Int) {
    LEFT_CLICK(GLFW_MOUSE_BUTTON_LEFT),
    RIGHT_CLICK(GLFW_MOUSE_BUTTON_RIGHT),
    SCROLL_CLICK(GLFW.GLFW_MOUSE_BUTTON_MIDDLE);

    companion object {
        fun getButtonByValue(glfwValue: Int): MouseButton? {
            return MouseButton.values().firstOrNull { it.glfwValue == glfwValue }
        }
    }
}