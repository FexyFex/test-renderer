package me.fexus.vulkan

import me.fexus.memory.OffHeapSafeAllocator
import me.fexus.vulkan.layer.VulkanLayer
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.vulkan.VK10.vkDestroyInstance
import org.lwjgl.vulkan.VK12
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo


class Instance() {
    lateinit var vkInstance: VkInstance


    fun create(layers: List<VulkanLayer>): Instance {
        this.vkInstance = OffHeapSafeAllocator.runMemorySafe {
            val appInfo = calloc<VkApplicationInfo>() {
                sType(VK12.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                pNext(0)
                apiVersion(VK12.VK_API_VERSION_1_2)
                applicationVersion(VK12.VK_MAKE_VERSION(1, 0, 0))
                pApplicationName(allocateString("Test Renderer"))
                pEngineName(allocateString("Test Engine"))
                engineVersion(VK12.VK_MAKE_VERSION(1, 0, 0))
            }

            val ppEnabledLayers = allocatePointer(layers.size)
            layers.forEachIndexed { index, vulkanLayer ->
                ppEnabledLayers.put(index, allocateString(vulkanLayer.name))
            }

            val glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions() ?: throw Exception()
            val ppEnabledExtensions = allocatePointer(glfwExtensions.capacity())
            repeat(glfwExtensions.capacity()) {
                ppEnabledExtensions.put(it, glfwExtensions[it])
            }

            val instanceCreateInfo = calloc<VkInstanceCreateInfo>() {
                sType(VK12.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                pNext(0)
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
        vkDestroyInstance(vkInstance, null)
    }
}