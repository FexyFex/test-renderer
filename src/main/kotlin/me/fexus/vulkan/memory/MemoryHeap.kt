package me.fexus.vulkan.memory

import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlags

data class MemoryHeap(
    val index: Int,
    val memoryPropertyFlags: MemoryPropertyFlags,
    val totalSize: Long,
    val budget: Long,
    val memoryTypes: List<MemoryType>
)