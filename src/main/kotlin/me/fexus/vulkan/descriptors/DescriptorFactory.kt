package me.fexus.vulkan.descriptors

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.component.Device
import me.fexus.vulkan.component.PhysicalDevice
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlags
import me.fexus.vulkan.memory.MemoryStatistics
import me.fexus.vulkan.memory.budget.MemoryHeapTypeFinder
import org.lwjgl.vulkan.VK12
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties


interface DescriptorFactory {
    var memoryStatistics: MemoryStatistics
    var memoryFinder: MemoryHeapTypeFinder
    var physicalDevice: PhysicalDevice
    var device: Device


    fun findMemoryTypeIndex(typeBits: Int, memProps: MemoryPropertyFlags): Int {
        return runMemorySafe {
            val deviceMemProps = calloc(VkPhysicalDeviceMemoryProperties::calloc)
            VK12.vkGetPhysicalDeviceMemoryProperties(physicalDevice.vkHandle, deviceMemProps)

            for (i in 0 until deviceMemProps.memoryTypeCount()) {
                val propertyFlags = deviceMemProps.memoryTypes(i).propertyFlags()
                val typeBitsSatisfied = (typeBits and (1 shl i) != 0)
                val containsRequestedProperties = propertyFlags and memProps.vkBits == memProps.vkBits
                if (typeBitsSatisfied && containsRequestedProperties) return@runMemorySafe i
            }

            throw Exception()
        }
    }
}