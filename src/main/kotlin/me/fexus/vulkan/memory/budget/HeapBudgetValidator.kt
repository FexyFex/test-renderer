package me.fexus.vulkan.memory.budget

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.component.PhysicalDevice
import me.fexus.vulkan.memory.MemoryAnalyzer
import me.fexus.vulkan.memory.MemoryHeap
import org.lwjgl.vulkan.EXTMemoryBudget.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryBudgetPropertiesEXT
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties2


class HeapBudgetValidator {
    private lateinit var physicalDevice: PhysicalDevice
    private lateinit var memoryAnalyzer: MemoryAnalyzer


    fun create(physicalDevice: PhysicalDevice, memoryAnalyzer: MemoryAnalyzer) {
        this.physicalDevice = physicalDevice
        this.memoryAnalyzer = memoryAnalyzer
    }

    fun validateBudget(physicalDevice: PhysicalDevice, size: Long, heap: MemoryHeap): HeapBudgetReport = runMemorySafe {
        val budgetProps = calloc(VkPhysicalDeviceMemoryBudgetPropertiesEXT::calloc) {
            sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT)
        }
        val props2 = calloc(VkPhysicalDeviceMemoryProperties2::calloc) {
            sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PROPERTIES_2)
            pNext(budgetProps.address())
        }
        vkGetPhysicalDeviceMemoryProperties2(physicalDevice.vkHandle, props2)

        val budget = budgetProps.heapBudget(heap.index)
        val budgetSufficient = budget >= size

        return@runMemorySafe HeapBudgetReport(memoryAnalyzer, heap, budgetSufficient)
    }
}