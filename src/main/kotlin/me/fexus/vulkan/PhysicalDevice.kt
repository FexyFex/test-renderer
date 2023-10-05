package me.fexus.vulkan

import me.fexus.memory.OffHeapSafeAllocator
import org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices
import org.lwjgl.vulkan.VkPhysicalDevice


class PhysicalDevice {
    lateinit var vkPhysicalDevice: VkPhysicalDevice

    fun create(instance: Instance, pickCriteria: PickCriteria = PickCriteria.BEST_DEVICE): PhysicalDevice {
        this.vkPhysicalDevice = OffHeapSafeAllocator.runMemorySafe {
            val pDeviceCount = allocateInt(1)
            vkEnumeratePhysicalDevices(instance.vkInstance, pDeviceCount, null)
            val deviceCount = pDeviceCount[0]
            if (deviceCount <= 0) throw Exception()

            val ppDevices = allocatePointer(deviceCount)
            vkEnumeratePhysicalDevices(instance.vkInstance, pDeviceCount, ppDevices)

            // TODO: Consider PickCriteria
            val physicalDeviceHandle = ppDevices[0]
            return@runMemorySafe VkPhysicalDevice(physicalDeviceHandle, instance.vkInstance)
        }
        return this
    }


    fun destroy() {

    }


    enum class PickCriteria {
        BEST_DEVICE,
        WORST_DEVICE,
        DONT_CARE
    }
}