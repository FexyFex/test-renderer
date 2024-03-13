package me.fexus.window.input

import me.fexus.math.vec.IVec2
import me.fexus.math.vec.Vec2
import me.fexus.window.Window
import me.fexus.window.input.event.InputEventPoster
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWCharCallbackI
import org.lwjgl.glfw.GLFWCursorPosCallbackI
import org.lwjgl.glfw.GLFWKeyCallbackI
import org.lwjgl.glfw.GLFWMouseButtonCallbackI


class InputHandler(private val window: Window): InputEventPoster() {
    private val downKeys = mutableListOf<Key>()
    private val downMouseButtons = mutableListOf<MouseButton>()

    init {
        val keyCallback = GLFWKeyCallbackI { _, key: Int, scancode: Int, action: Int, _ ->
            if(action == GLFW.GLFW_PRESS) {
                val pressedKey = Key.getKeyByValue(key)
                if (pressedKey != null) {
                    downKeys.add(pressedKey)
                    postKeyPressed(pressedKey)
                }
            }

            if (action == GLFW.GLFW_RELEASE) {
                val releasedKey = Key.getKeyByValue(key)
                if (releasedKey != null) {
                    downKeys.remove(releasedKey)
                    postKeyReleased(releasedKey)
                }
            }
        }
        GLFW.glfwSetKeyCallback(window.handle, keyCallback)

        val mouseCallback = GLFWMouseButtonCallbackI { _, button: Int, action: Int, _ ->
            if (action == GLFW.GLFW_PRESS) {
                val pressedButton = MouseButton.getButtonByValue(button)
                if (pressedButton != null) {
                    downMouseButtons.add(pressedButton)
                    postMouseButtonPressed(pressedButton)
                }
            }

            if (action == GLFW.GLFW_RELEASE) {
                val releasedButton = MouseButton.getButtonByValue(button)
                if (releasedButton != null) {
                    downMouseButtons.remove(releasedButton)
                    postMouseButtonReleased(releasedButton)
                }
            }
        }
        GLFW.glfwSetMouseButtonCallback(window.handle, mouseCallback)

        val charCallback = GLFWCharCallbackI { _, charCode ->
            postCharTyped(charCode.toChar())
        }
        GLFW.glfwSetCharCallback(window.handle, charCallback)

        val cursorPosCallback = GLFWCursorPosCallbackI { _, x: Double, y: Double ->
            postMouseMoved(IVec2(x.toInt(), y.toInt()))
        }
        GLFW.glfwSetCursorPosCallback(window.handle, cursorPosCallback)
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