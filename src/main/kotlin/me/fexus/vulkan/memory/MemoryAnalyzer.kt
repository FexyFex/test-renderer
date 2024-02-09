package me.fexus.vulkan.memory

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.component.PhysicalDevice
import me.fexus.vulkan.descriptors.memorypropertyflags.CombinedMemoryPropertyFlags
import org.lwjgl.vulkan.VK12
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties2


class MemoryAnalyzer {
    private lateinit var physicalDevice: PhysicalDevice

    private val mutHeaps = mutableListOf<MemoryHeap>()
    private val mutTypes = mutableListOf<MemoryType>()
    val heaps: List<MemoryHeap>; get() = mutHeaps
    val types: List<MemoryType>; get() = mutTypes


    fun create(physicalDevice: PhysicalDevice) {
        this.physicalDevice = physicalDevice

        runMemorySafe {
            val props2 = calloc(VkPhysicalDeviceMemoryProperties2::calloc) {
                sType(VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PROPERTIES_2)
            }
            VK12.vkGetPhysicalDeviceMemoryProperties2(physicalDevice.vkHandle, props2)

            val memoryHeapCount = props2.memoryProperties().memoryHeapCount()

            for (heapIndex in 0 until memoryHeapCount) {
                val vkHeap = props2.memoryProperties().memoryHeaps(heapIndex)
                val memoryTypes = props2.memoryProperties().memoryTypes()
                    .filter { it.heapIndex() == heapIndex }
                    .mapIndexed { typeIndex, vkMemoryType ->
                        MemoryType(typeIndex, CombinedMemoryPropertyFlags(vkMemoryType.propertyFlags()))
                    }

                val heap = MemoryHeap(
                    heapIndex,
                    CombinedMemoryPropertyFlags(vkHeap.flags()),
                    vkHeap.size(),
                    memoryTypes
                )

                this@MemoryAnalyzer.mutHeaps.add(heap)
            }

            for (typeIndex in 0 until props2.memoryProperties().memoryTypeCount()) {
                val vkMemoryType = props2.memoryProperties().memoryTypes(typeIndex)
                val memoryType = MemoryType(
                    typeIndex, CombinedMemoryPropertyFlags(vkMemoryType.propertyFlags())
                )
                this@MemoryAnalyzer.mutTypes.add(memoryType)
            }
        }
    }
}