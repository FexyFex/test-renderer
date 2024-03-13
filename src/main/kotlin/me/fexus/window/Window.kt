package me.fexus.window

import me.fexus.math.vec.IVec2
import org.lwjgl.glfw.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryUtil.NULL
import java.nio.ByteBuffer
import kotlin.math.roundToInt


class Window(title: String, config: WindowConfigContext.() -> Unit) {
    val handle: Long

    var title: String = title; private set
    val extent2D: IVec2
        get() {
            val pWidth = IntArray(1) { 0 }
            val pHeight = IntArray(1) { 0 }
            glfwGetFramebufferSize(this.handle, pWidth, pHeight)

            return IVec2(pWidth.first(), pHeight.first())
        }

    var requiresResize = true
        private set
    private var fullscreen = false
    val shouldClose: Boolean
        get() = glfwWindowShouldClose(handle)

    val aspect: Float
        get() {
            val extent = extent2D
            return (extent.x.toFloat() / extent.y.toFloat())
        }

    val cursorPos: IVec2
        get() {
            val xPos = doubleArrayOf(0.0)
            val yPos = doubleArrayOf(0.0)
            glfwGetCursorPos(handle, xPos, yPos)
            return IVec2(xPos[0].roundToInt(), yPos[0].roundToInt())
        }

    var minimumWidth = GLFW_DONT_CARE; private set // GLFW_DONT_CARE = -1
    var minimumHeight = GLFW_DONT_CARE; private set
    var maximumWidth = GLFW_DONT_CARE; private set
    var maximumHeight = GLFW_DONT_CARE; private set
    internal var initialWidth = 800
    internal var initialHeight = 600
    internal var initialPosX = 0
    internal var initialPosY = 0
    private var previousWidth = 0
    private var previousHeight = 0
    private var minimumAspect = 0f

    private var frameBufferResizeAction: (previousWidth: Int, previousHeight: Int, width: Int, height: Int) -> Unit = { _, _, _, _->  }


    init {
        if (!glfwInit()) throw Error("Failed to initialize GLFW")
        if (!GLFWVulkan.glfwVulkanSupported()) throw Exception()

        WindowConfigContext(this).config()

        launchOptionWithoutAPI() // API is OpenGL by default, but that is big no no

        val videomode = glfwGetVideoMode(glfwGetPrimaryMonitor())
            ?: throw AssertionError("No Video Mode found for primary monitor")
        handle = glfwCreateWindow(initialWidth, initialHeight, title, NULL, NULL)
        updateSizeLimit()
        glfwSetWindowMonitor(handle, NULL, initialPosX, initialPosY, initialWidth, initialHeight, GLFW_DONT_CARE)

        previousWidth = initialWidth
        previousHeight = initialHeight
        /*
val e = GLFWWindowRefreshCallbackI {
     val extent = extent2D
     frameBufferResizeAction(previousWidth, previousHeight, extent.width, extent.height)

     requiresResize = true
     previousWidth = extent.width
     previousHeight = extent.height
 }
  */
        //glfwSetWindowRefreshCallback(handle, e)
        val framebufferSizeCallback = GLFWFramebufferSizeCallbackI { _, width, height ->
            frameBufferResizeAction(previousWidth, previousHeight, width, height)

            requiresResize = true
            previousWidth = width
            previousHeight = height
        }
        glfwSetFramebufferSizeCallback(handle, framebufferSizeCallback)

        waitForFramebufferResize()
    }

    internal fun waitEvents() {
        glfwWaitEvents()
    }

    internal fun waitEvents(timeOut: Double) {
        glfwWaitEventsTimeout(timeOut)
    }

    internal fun pollEvents() {
        glfwPollEvents()
    }

    internal fun enableVsync(): Window {
        glfwSwapInterval(1)
        return this
    }
    internal fun disableVsync(): Window {
        glfwSwapInterval(0)
        return this
    }

    fun launchOptionWithoutAPI(): Window {
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        return this
    }

    internal fun enableGLFWDebugContext(): Window {
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE)
        return this
    }
    internal fun disableGLFWDebugContext(): Window {
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE)
        return this
    }

    fun keepWindowAspect(numerator: Int, denominator: Int): Window {
        glfwSetWindowAspectRatio(handle, numerator, denominator)
        return this
    }

    private fun updateSizeLimit() {
        glfwSetWindowSizeLimits(handle, minimumWidth, minimumHeight, maximumWidth, maximumHeight)
    }

    fun setCursorPosCallback(callback: GLFWCursorPosCallbackI) = glfwSetCursorPosCallback(handle, callback)
    fun setMouseButtonCallback(callback: GLFWMouseButtonCallbackI) = glfwSetMouseButtonCallback(handle, callback)
    fun setCharCallback(callback: GLFWCharCallbackI) = glfwSetCharCallback(handle, callback)
    fun setKeyCallback(callback: GLFWKeyCallbackI) = glfwSetKeyCallback(handle, callback)

    fun toggleBorderlessFullscreen() {
        fullscreen = !fullscreen

        val videomode = glfwGetVideoMode(glfwGetPrimaryMonitor())
            ?: throw AssertionError("No Video Mode found for primary monitor")

        if (fullscreen) {
            val width = videomode.width()
            val height = videomode.height()
            setSize(width, height)
            glfwSetWindowMonitor(handle, glfwGetPrimaryMonitor(), 0, 0, width, height, GLFW_DONT_CARE)
        } else {
            val width = videomode.width() / 2 - 350
            val height = 50
            setSize(width, height)
            glfwSetWindowMonitor(handle, NULL, width, height, 700,1000, GLFW_DONT_CARE)
        }
    }

    fun setCursorPos(pos: IVec2) {
        glfwSetCursorPos(this.handle, pos.x.toDouble(), pos.y.toDouble())
    }

    fun hideCursor() {
        glfwSetInputMode(this.handle, GLFW_CURSOR, GLFW_CURSOR_HIDDEN)
    }

    fun showCursor() {
        glfwSetInputMode(this.handle, GLFW_CURSOR, GLFW_CURSOR_NORMAL)
    }

    fun waitForFramebufferResize() {
        val pWidth = IntArray(1) { 0 }
        val pHeight = IntArray(1) { 0 }
        glfwGetFramebufferSize(this.handle, pWidth, pHeight)
        while (pWidth.first() == 0 || pHeight.first() == 0) {
            glfwGetFramebufferSize(this.handle, pWidth, pHeight)
            glfwWaitEvents()
        }

        requiresResize = false
    }

    fun setShouldClose(){
        glfwSetWindowShouldClose(handle, true)
    }

    fun destroy() {
        glfwDestroyWindow(handle)
        glfwTerminate()
    }

    fun setSize(width: Int, height: Int) {
        glfwSetWindowSize(handle, width, height)
        requiresResize = true
    }

    fun setTitle(title: String) {
        this.title = title
        glfwSetWindowTitle(handle, title)
    }

    fun setTitleRememberFormer(title: String) {
        glfwSetWindowTitle(handle, title)
    }

    fun setWindowIcon(pixels: ByteBuffer, width: Int, height: Int) {
        val windowIcon = GLFWImage.calloc(1)
            .width(width)
            .height(height)
            .pixels(pixels)
        glfwSetWindowIcon(handle, windowIcon)
    }


    data class Extent2D(val width: Int, val height: Int)

    class WindowConfigContext internal constructor(private val window: Window) {
        fun enableResizable() = glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
        fun disableResizable() = glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)

        fun windowVisible() = glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE)
        fun windowInvisible() = glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)

        fun enableDecoration() = glfwWindowHint(GLFW_DECORATED, GLFW_TRUE)
        fun disableDecoration() = glfwWindowHint(GLFW_DECORATED, GLFW_FALSE)

        fun enableTransparentFrameBuffer() = glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_TRUE)
        fun disableTransparentFrameBuffer() = glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_FALSE)

        fun enableAutoIconify() = glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_TRUE)
        fun disableAutoIconification() = glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE)

        fun setInitialWindowSize(width: Int, height: Int) {
            window.initialWidth = width
            window.initialHeight = height
        }

        fun setInitialWindowPosition(x: Int, y: Int) {
            window.initialPosX = x
            window.initialPosY = y
        }
    }
}