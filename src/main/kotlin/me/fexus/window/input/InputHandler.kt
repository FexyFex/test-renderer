package me.fexus.window.input

import me.fexus.math.vec.Vec2
import me.fexus.window.Window
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWKeyCallbackI
import org.lwjgl.glfw.GLFWMouseButtonCallbackI


class InputHandler(private val window: Window) {
    private val downKeys = mutableListOf<Key>()
    private val downMouseButtons = mutableListOf<MouseButton>()

    init {
        val keyCallback = GLFWKeyCallbackI { _, key: Int, scancode: Int, action: Int, _ ->
            if(action == GLFW.GLFW_PRESS) {
                val pressedKey = Key.getKeyByValue(key)
                if (pressedKey != null) {
                    downKeys.add(pressedKey)
                }
            }

            if (action == GLFW.GLFW_RELEASE) {
                val releasedKey = Key.getKeyByValue(key)
                if (releasedKey != null) {
                    downKeys.remove(releasedKey)
                }
            }
        }
        GLFW.glfwSetKeyCallback(window.handle, keyCallback)

        val mouseCallback = GLFWMouseButtonCallbackI { _, button: Int, action: Int, _ ->
            if (action == GLFW.GLFW_PRESS) {
                val pressedButton = MouseButton.getButtonByValue(button)
                if (pressedButton != null) {
                    downMouseButtons.add(pressedButton)
                }
            }

            if (action == GLFW.GLFW_RELEASE) {
                val releasedButton = MouseButton.getButtonByValue(button)
                if (releasedButton != null) {
                    downMouseButtons.remove(releasedButton)
                }
            }
        }
        GLFW.glfwSetMouseButtonCallback(window.handle, mouseCallback)
    }

    fun isKeyDown(key: Key): Boolean = key in downKeys

    fun isMouseButtonDown(button: MouseButton): Boolean = button in downMouseButtons

    fun getCursorPos(): Vec2 {
        val xPos = DoubleArray(1) { 0.0 }
        val yPos = DoubleArray(1) { 0.0 }
        GLFW.glfwGetCursorPos(window.handle, xPos, yPos)
        return Vec2(xPos[0].toFloat(), yPos[0].toFloat())
    }
}