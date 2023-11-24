package me.fexus.vulkan.component

import me.fexus.memory.runMemorySafe
import org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices
import org.lwjgl.vulkan.VkPhysicalDevice


class PhysicalDevice {
    lateinit var vkHandle: VkPhysicalDevice

    fun create(instance: Instance, pickCriteria: PickCriteria = PickCriteria.BEST_DEVICE): PhysicalDevice {
        this.vkHandle = runMemorySafe {
            val pDeviceCount = allocateInt(1)
            vkEnumeratePhysicalDevices(instance.vkHandle, pDeviceCount, null)
            val deviceCount = pDeviceCount[0]
            println("Device found: $deviceCount")
            if (deviceCount <= 0) throw Exception()

            val ppDevices = allocatePointer(deviceCount)
            vkEnumeratePhysicalDevices(instance.vkHandle, pDeviceCount, ppDevices)

            // TODO: Consider PickCriteria
            val physicalDeviceHandle = ppDevices[0]
            return@runMemorySafe VkPhysicalDevice(physicalDeviceHandle, instance.vkHandle)
        }
        return this
    }


    enum class PickCriteria {
        BEST_DEVICE,
        WORST_DEVICE,
        DONT_CARE
    }
}