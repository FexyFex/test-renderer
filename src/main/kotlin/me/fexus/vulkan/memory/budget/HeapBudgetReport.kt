package me.fexus.vulkan.memory.budget

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.component.PhysicalDevice
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlags
import me.fexus.vulkan.memory.MemoryAnalyzer
import me.fexus.vulkan.memory.MemoryHeap
import org.lwjgl.vulkan.EXTMemoryBudget
import org.lwjgl.vulkan.VK12
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryBudgetPropertiesEXT
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties2


class HeapBudgetReport(
    private val memoryAnalyzer: MemoryAnalyzer,
    val heap: MemoryHeap,
    val memoryProperyFlags: MemoryPropertyFlags,
    val budgetSufficient: Boolean,
) {

    fun HeapBudgetReport.suggestAlternative(): HeapBudgetReport = runMemorySafe {
        val budgetProps = calloc(VkPhysicalDeviceMemoryBudgetPropertiesEXT::calloc) {
            sType(EXTMemoryBudget.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT)
        }
        val props2 = calloc(VkPhysicalDeviceMemoryProperties2::calloc) {
            sType(VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PROPERTIES_2)
            pNext(budgetProps.address())
        }
        VK12.vkGetPhysicalDeviceMemoryProperties2(physicalDevice.vkHandle, props2)

        val heaps = props2.memoryProperties().memoryHeaps()
        val heapBudgets = budgetProps.heapBudget()

        for (i in 0 until props2.memoryProperties().memoryHeapCount()) {

        }
        heaps.forEachIndexed { index, vkMemoryHeap ->
            // Skip the heap of this BudgetReport. Obviously, since we are looking
            // for an alternative, we won't want to get it again.
            if (index == this@HeapBudgetReport.heap.index) return@forEachIndexed

            val budget = heapBudgets[index]

            vkMemoryHeap.flags()
        }

        return@runMemorySafe HeapBudgetReport(physicalDevice, )
    }
}