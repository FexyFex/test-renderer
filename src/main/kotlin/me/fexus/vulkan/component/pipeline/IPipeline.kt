package me.fexus.vulkan.component.pipeline

import me.fexus.memory.OffHeapSafeAllocator
import me.fexus.vulkan.component.Device
import org.lwjgl.vulkan.VK12
import org.lwjgl.vulkan.VkShaderModuleCreateInfo


interface IPipeline {
    val vkHandle: Long
    val vkLayoutHandle: Long

    fun createShaderModule(device: Device, shaderCode: ByteArray): Long = OffHeapSafeAllocator.runMemorySafe {
        val pCode = allocate(shaderCode.size)
        shaderCode.forEachIndexed { index, byte -> pCode.put(index, byte) }

        val moduleCreateInfo = calloc(VkShaderModuleCreateInfo::calloc) {
            sType(VK12.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
            pNext(0)
            pCode(pCode)
            flags(0)
        }

        val pShaderModule = allocateLong(1)
        VK12.vkCreateShaderModule(device.vkHandle, moduleCreateInfo, null, pShaderModule)
        return@runMemorySafe pShaderModule[0]
    }
}