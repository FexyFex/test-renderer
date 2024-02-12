package me.fexus.vulkan.memory

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.component.PhysicalDevice
import me.fexus.vulkan.descriptors.memorypropertyflags.CombinedMemoryPropertyFlags
import org.lwjgl.vulkan.EXTMemoryBudget.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT
import org.lwjgl.vulkan.VK12
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryBudgetPropertiesEXT
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties2


class MemoryStatistics {
    private lateinit var physicalDevice: PhysicalDevice

    private val mutMemoryHeaps = mutableListOf<MemoryHeap>()
    private val mutMemoryTypes = mutableListOf<MemoryType>()
    val memoryHeaps: List<MemoryHeap>; get() = mutMemoryHeaps
    val memoryTypes: List<MemoryType>; get() = mutMemoryTypes


    fun create(physicalDevice: PhysicalDevice) {
        this.physicalDevice = physicalDevice

        runMemorySafe {
            val budgetProps = calloc(VkPhysicalDeviceMemoryBudgetPropertiesEXT::calloc) {
                sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT)
            }
            val memoryProps2 = calloc(VkPhysicalDeviceMemoryProperties2::calloc) {
                sType(VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PROPERTIES_2)
                pNext(budgetProps.address())
            }
            VK12.vkGetPhysicalDeviceMemoryProperties2(physicalDevice.vkHandle, memoryProps2)

            val memoryHeapCount = memoryProps2.memoryProperties().memoryHeapCount()

            for (heapIndex in 0 until memoryHeapCount) {
                val vkHeap = memoryProps2.memoryProperties().memoryHeaps(heapIndex)
                val memoryTypes = memoryProps2.memoryProperties().memoryTypes()
                    .filter { it.heapIndex() == heapIndex }
                    .mapIndexed { typeIndex, vkMemoryType ->
                        MemoryType(
                            typeIndex,
                            vkMemoryType.heapIndex(),
                            CombinedMemoryPropertyFlags(vkMemoryType.propertyFlags())
                        )
                    }

                val heap = MemoryHeap(
                    heapIndex,
                    CombinedMemoryPropertyFlags(vkHeap.flags()),
                    vkHeap.size(),
                    budgetProps.heapBudget(heapIndex),
                    memoryTypes
                )

                this@MemoryStatistics.mutMemoryHeaps.add(heap)
            }

            for (typeIndex in 0 until memoryProps2.memoryProperties().memoryTypeCount()) {
                val vkMemoryType = memoryProps2.memoryProperties().memoryTypes(typeIndex)
                val memoryType = MemoryType(
                    typeIndex, vkMemoryType.heapIndex(), CombinedMemoryPropertyFlags(vkMemoryType.propertyFlags())
                )
                this@MemoryStatistics.mutMemoryTypes.add(memoryType)
            }
        }
    }
}
