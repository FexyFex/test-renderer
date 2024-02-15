package me.fexus.vulkan.memory.budget

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.component.PhysicalDevice
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlags
import me.fexus.vulkan.memory.MemoryStatistics
import me.fexus.vulkan.memory.MemoryHeap
import me.fexus.vulkan.memory.MemoryType
import org.lwjgl.vulkan.EXTMemoryBudget
import org.lwjgl.vulkan.VK12
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryBudgetPropertiesEXT
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties2


open class MemoryTypeSearchReport(
    private val physicalDevice: PhysicalDevice,
    private val memoryStatistics: MemoryStatistics,
    private val requiredSize: Long,
    private val allowedMemoryPropertyFlags: MemoryPropertyFlags,
    private val preferredMemoryPropertyFlags: MemoryPropertyFlags,
    private val disallowedHeaps: List<MemoryHeap>,
    val heap: MemoryHeap,
    val type: MemoryType,
    val heapBudgetSufficient: Boolean,
    val noMoreMemoryAvailable: Boolean
) {

    fun suggestAlternative(): MemoryTypeSearchReport = runMemorySafe {
        val budgetProps = calloc(VkPhysicalDeviceMemoryBudgetPropertiesEXT::calloc) {
            sType(EXTMemoryBudget.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT)
        }
        val memProps = calloc(VkPhysicalDeviceMemoryProperties2::calloc) {
            sType(VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PROPERTIES_2)
            pNext(budgetProps.address())
        }
        VK12.vkGetPhysicalDeviceMemoryProperties2(physicalDevice.vkHandle, memProps)

        val disallowedHeapIndices = (disallowedHeaps.map { it.index } + heap.index)
        val otherMemorTypes = memoryStatistics.memoryTypes.filter { it.heapIndex !in disallowedHeapIndices }

        // This will happen if all memory heaps are used up. Final destination...
        if (otherMemorTypes.isEmpty()) {
            return@runMemorySafe MemoryTypeSearchReport(
                physicalDevice,
                memoryStatistics,
                requiredSize,
                allowedMemoryPropertyFlags,
                preferredMemoryPropertyFlags,
                disallowedHeaps + heap,
                heap,
                type,
                heapBudgetSufficient = false,
                noMoreMemoryAvailable = true
            )
        }
        val closestMemoryType = otherMemorTypes.firstOrNull {
            it.memoryPropertyFlags.contains(preferredMemoryPropertyFlags) &&
                    budgetProps.heapBudget(it.heapIndex) - budgetProps.heapUsage(it.heapIndex) >= requiredSize
        }
        // if no closest type was found, we just take the one with the biggest budget
        val finalMemoryType = closestMemoryType ?: otherMemorTypes.maxBy { type ->
            budgetProps.heapBudget(type.heapIndex) - budgetProps.heapUsage(type.heapIndex)
        }
        val finalMemoryHeap = memoryStatistics.memoryHeaps.first { it.index == finalMemoryType.heapIndex }
        val budget = budgetProps.heapBudget(finalMemoryHeap.index)
        val usage = budgetProps.heapUsage(finalMemoryHeap.index)
        val budgetSufficient = (budget - usage) >= requiredSize

        return@runMemorySafe MemoryTypeSearchReport(
            physicalDevice,
            memoryStatistics,
            requiredSize,
            allowedMemoryPropertyFlags,
            preferredMemoryPropertyFlags,
            disallowedHeaps + finalMemoryHeap,
            finalMemoryHeap,
            finalMemoryType,
            budgetSufficient,
            noMoreMemoryAvailable = false
        )
    }
}