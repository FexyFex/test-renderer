package me.fexus.vulkan.component

import me.fexus.memory.OffHeapSafeAllocator
import me.fexus.vulkan.component.debug.DebugUtilsMessenger
import me.fexus.vulkan.layer.VulkanLayer
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.vkDestroyInstance


class Instance() {
    lateinit var vkHandle: VkInstance


    fun create(layers: List<VulkanLayer>): Instance {
        this.vkHandle = OffHeapSafeAllocator.runMemorySafe {
            val appInfo = calloc(VkApplicationInfo::calloc) {
                sType(VK12.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                pNext(0)
                apiVersion(VK12.VK_API_VERSION_1_2)
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
            val ppEnabledExtensions = allocatePointer(glfwExtensions.capacity() + 1)
            repeat(glfwExtensions.capacity()) {
                ppEnabledExtensions.put(it, glfwExtensions[it])
            }
            ppEnabledExtensions.put(ppEnabledExtensions.capacity() - 1, allocateStringValue("VK_EXT_debug_utils"))

            val debugCreateInfo = DebugUtilsMessenger.getCreateInfo(this)

            val instanceCreateInfo = calloc(VkInstanceCreateInfo::calloc) {
                sType(VK12.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                pNext(debugCreateInfo.address())
                flags(0)
                pApplicationInfo(appInfo)
                ppEnabledExtensionNames(ppEnabledExtensions)
                ppEnabledLayerNames(ppEnabledLayers)
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