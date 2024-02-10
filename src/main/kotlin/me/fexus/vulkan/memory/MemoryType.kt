package me.fexus.vulkan.memory

import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlags


data class MemoryType(val index: Int, val heapIndex: Int, val memoryPropertyFlags: MemoryPropertyFlags)