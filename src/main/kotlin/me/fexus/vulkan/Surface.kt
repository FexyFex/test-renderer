package me.fexus.vulkan

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.window.Window
import org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface


class Surface {
    var vkHandle: Long = 0

    fun create(instance: Instance, window: Window): Surface {
        vkHandle = runMemorySafe {
            val pSurfaceHandle = allocateLong(1)
            glfwCreateWindowSurface(instance.vkInstance, window.handle, null, pSurfaceHandle)
            return@runMemorySafe pSurfaceHandle.get(0)
        }
        return this
    }
}