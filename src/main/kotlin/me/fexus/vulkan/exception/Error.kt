package me.fexus.vulkan.exception

import org.lwjgl.vulkan.VK10


inline fun Int.catchVK(catchBlock: (Int) -> Unit = { throw VulkanException(it) }) {
    if(this != VK10.VK_SUCCESS)
        catchBlock(this)
}