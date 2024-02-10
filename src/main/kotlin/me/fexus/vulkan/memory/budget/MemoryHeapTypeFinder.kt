package me.fexus.vulkan.memory.budget

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.component.PhysicalDevice
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlags
import me.fexus.vulkan.memory.MemoryStatistics
import me.fexus.vulkan.memory.MemoryType
import org.lwjgl.vulkan.EXTMemoryBudget.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryBudgetPropertiesEXT
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties2


class MemoryHeapTypeFinder {
    private lateinit var physicalDevice: PhysicalDevice
    private lateinit var memoryStatistic: MemoryStatistics


    fun create(physicalDevice: PhysicalDevice, memoryAnalyzer: MemoryStatistics) {
        this.physicalDevice = physicalDevice
        this.memoryStatistic = memoryAnalyzer
    }

    fun findMemoryType(
        size: Long,
        allowedMemoryPropertyFlagBits: MemoryPropertyFlags,
        preferredMemoryProperyFlags: MemoryPropertyFlags
    ) = runMemorySafe {
        val budgetProps = calloc(VkPhysicalDeviceMemoryBudgetPropertiesEXT::calloc) {
            sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT)
        }
        val memProps = calloc(VkPhysicalDeviceMemoryProperties2::calloc) {
            sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PROPERTIES_2)
            pNext(budgetProps.address())
        }
        vkGetPhysicalDeviceMemoryProperties2(physicalDevice.vkHandle, memProps)

        val firstMemoryType = findMemoryTypeByProperties(allowedMemoryPropertyFlagBits, preferredMemoryProperyFlags)

        val budget = budgetProps.heapBudget(firstMemoryType.heapIndex)
        val heap = memoryStatistic.memoryHeaps[firstMemoryType.heapIndex]
        val budgetSufficient = budget >= size

        return@runMemorySafe MemoryTypeSearchReport(
            physicalDevice,
            memoryStatistic,
            size,
            allowedMemoryPropertyFlagBits,
            preferredMemoryProperyFlags,
            emptyList(),
            heap,
            firstMemoryType,
            budgetSufficient
        )
    }


    private fun findMemoryTypeByProperties(
        allowedPropertyFlags: MemoryPropertyFlags,
        requestedPropertyFlags: MemoryPropertyFlags
    ): MemoryType {
        for (type in memoryStatistic.memoryTypes) {
            val containsAllPropertyFlags = type.memoryPropertyFlags.contains(requestedPropertyFlags)
            val allowedPropertiesSatisfied = allowedPropertyFlags.contains(type.memoryPropertyFlags.vkBits)
            if (containsAllPropertyFlags && allowedPropertiesSatisfied) return type
        }
        throw Exception()
    }
}