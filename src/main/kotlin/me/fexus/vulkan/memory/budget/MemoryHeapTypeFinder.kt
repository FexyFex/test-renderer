package me.fexus.vulkan.memory.budget

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.component.PhysicalDevice
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlag
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlags
import me.fexus.vulkan.memory.MemoryStatistics
import me.fexus.vulkan.memory.MemoryType
import org.lwjgl.vulkan.EXTMemoryBudget.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryBudgetPropertiesEXT
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties2


class MemoryHeapTypeFinder {
    private lateinit var physicalDevice: PhysicalDevice
    private lateinit var memoryStatistics: MemoryStatistics


    fun create(physicalDevice: PhysicalDevice, memoryAnalyzer: MemoryStatistics) {
        this.physicalDevice = physicalDevice
        this.memoryStatistics = memoryAnalyzer
    }


    fun findMemoryType(
        size: Long,
        allowedMemoryPropertyFlagBits: MemoryPropertyFlags,
        preferredMemoryProperyFlags: MemoryPropertyFlags
    ): MemoryTypeSearchReport = runMemorySafe {
        val budgetProps = calloc(VkPhysicalDeviceMemoryBudgetPropertiesEXT::calloc) {
            sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT)
        }

        val memoryProps = calloc(VkPhysicalDeviceMemoryProperties2::calloc) {
            sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PROPERTIES_2)
            pNext(budgetProps.address())
        }
        vkGetPhysicalDeviceMemoryProperties2(physicalDevice.vkHandle, memoryProps)

        val firstMemoryType = findMemoryTypeByProperties(allowedMemoryPropertyFlagBits, preferredMemoryProperyFlags)

        val budget = budgetProps.heapBudget(firstMemoryType.heapIndex)
        val usage = budgetProps.heapUsage(firstMemoryType.heapIndex)
        val heap = memoryStatistics.memoryHeaps[firstMemoryType.heapIndex]
        val budgetSufficient = (budget - usage) >= size

        return@runMemorySafe MemoryTypeSearchReport(
            physicalDevice,
            memoryStatistics,
            size,
            allowedMemoryPropertyFlagBits,
            preferredMemoryProperyFlags,
            emptyList(),
            heap,
            firstMemoryType,
            budgetSufficient,
            false
        )
    }


    fun findBARMemoryType(): MemoryType? = runMemorySafe {
        val budgetProps = calloc(VkPhysicalDeviceMemoryBudgetPropertiesEXT::calloc) {
            sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT)
        }

        val memoryProps = calloc(VkPhysicalDeviceMemoryProperties2::calloc) {
            sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PROPERTIES_2)
            pNext(budgetProps.address())
        }
        vkGetPhysicalDeviceMemoryProperties2(physicalDevice.vkHandle, memoryProps)

        var targetHeapIndex: Int = -1
        val requiredFlags = MemoryPropertyFlag.DEVICE_LOCAL + MemoryPropertyFlag.HOST_VISIBLE + MemoryPropertyFlag.HOST_COHERENT
        for (i in 0 until budgetProps.heapBudget().capacity()) {
            val heapBudget = budgetProps.heapBudget(i)
            val heap = memoryProps.memoryProperties().memoryHeaps(i)
            // BAR memory is typically 256MB with DEVICE_LOCAL, HOST_VISIBLE and HOST_COHERENT memory flags
            if (heapBudget < 400_000_000 && (heap.flags() and requiredFlags.vkBits == requiredFlags.vkBits)) {
                targetHeapIndex = i
            }
        }

        return@runMemorySafe memoryStatistics.memoryTypes.firstOrNull { it.heapIndex == targetHeapIndex }
    }


    private fun findMemoryTypeByProperties(
        allowedPropertyFlags: MemoryPropertyFlags,
        requestedPropertyFlags: MemoryPropertyFlags
    ): MemoryType {
        for ((index, type) in memoryStatistics.memoryTypes.withIndex()) {
            val containsAllPropertyFlags = type.memoryPropertyFlags.contains(requestedPropertyFlags)
            val allowedPropertiesSatisfied = allowedPropertyFlags.contains(1 shl index)
            if (containsAllPropertyFlags && allowedPropertiesSatisfied) return type
        }
        throw Exception()
    }
}