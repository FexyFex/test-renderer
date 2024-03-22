package me.fexus.vulkan.component

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.component.debug.DebugUtilsMessenger
import me.fexus.vulkan.extension.PhysicalDeviceProperties2KHR
import me.fexus.vulkan.layer.VulkanLayer
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.vkDestroyInstance
import org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3


class Instance() {
    lateinit var vkHandle: VkInstance


    fun create(layers: List<VulkanLayer>, withDebug: Boolean = false): Instance {
        this.vkHandle = runMemorySafe {
            val appInfo = calloc(VkApplicationInfo::calloc) {
                sType(VK12.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                pNext(0)
                apiVersion(VK_API_VERSION_1_3)
                applicationVersion(VK12.VK_MAKE_VERSION(1, 0, 0))
                pApplicationName(allocateStringValue("Test Renderer"))
                pEngineName(allocateStringValue("Test Engine"))
                engineVersion(VK12.VK_MAKE_VERSION(1, 0, 0))
            }

            val ppEnabledLayers = allocatePointer(layers.size)
            layers.forEachIndexed { index, vulkanLayer ->
                ppEnabledLayers.put(index, allocateStringValue(vulkanLayer.name))
            }

            val glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions() ?: throw Exception()
            val additionalExtensionCount = if (withDebug) 2 else 1
            val ppEnabledExtensions = allocatePointer(glfwExtensions.capacity() + additionalExtensionCount)
            repeat(glfwExtensions.capacity()) {
                ppEnabledExtensions.put(it, glfwExtensions[it])
            }
            if (withDebug) {
                ppEnabledExtensions.put(ppEnabledExtensions.capacity() - 2, allocateStringValue("VK_EXT_debug_utils"))
            }
            ppEnabledExtensions.put(ppEnabledExtensions.capacity() - 1, allocateStringValue(PhysicalDeviceProperties2KHR.name))

            val debugCreateInfo = DebugUtilsMessenger.getCreateInfo(this)

            val instanceCreateInfo = calloc(VkInstanceCreateInfo::calloc) {
                sType(VK12.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                if (withDebug) pNext(debugCreateInfo.address()) else pNext(0L)
                flags(0)
                pApplicationInfo(appInfo)
                ppEnabledExtensionNames(ppEnabledExtensions)
                if (withDebug) ppEnabledLayerNames(ppEnabledLayers)
            }

            val pInstance = allocatePointer(1)
            VK12.vkCreateInstance(instanceCreateInfo, null, pInstance)
            return@runMemorySafe VkInstance(pInstance.get(0), instanceCreateInfo)
        }

        return this
    }


    fun destroy() {
        vkDestroyInstance(vkHandle, null)
    }
}