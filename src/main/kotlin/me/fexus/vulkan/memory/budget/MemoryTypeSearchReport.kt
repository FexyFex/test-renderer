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

        val otherMemorTypes = memoryStatistics.memoryTypes.filter { it.heapIndex != heap.index }
        val closestMemoryType = otherMemorTypes.firstOrNull { it.memoryPropertyFlags.contains(preferredMemoryPropertyFlags) }
        val finalMemoryType = closestMemoryType // if no closest type was found, we just take the one with the biggest budget
            ?: otherMemorTypes.maxBy { type ->
                val heap = memoryStatistics.memoryHeaps.first {
                    it.index == type.heapIndex && allowedMemoryPropertyFlags.contains(type.memoryPropertyFlags)
                }
                budgetProps.heapBudget(heap.index)
            }
        val finalMemoryHeap = memoryStatistics.memoryHeaps.first { it.index == finalMemoryType.heapIndex }
        val budgetSufficient = budgetProps.heapBudget(finalMemoryHeap.index) >= requiredSize

        return@runMemorySafe MemoryTypeSearchReport(
            physicalDevice,
            memoryStatistics,
            requiredSize,
            allowedMemoryPropertyFlags,
            preferredMemoryPropertyFlags,
            disallowedHeaps + finalMemoryHeap,
            finalMemoryHeap,
            finalMemoryType,
            budgetSufficient
        )
    }
}