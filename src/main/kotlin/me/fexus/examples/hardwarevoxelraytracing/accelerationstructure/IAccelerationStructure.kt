package me.fexus.examples.hardwarevoxelraytracing.accelerationstructure

import me.fexus.vulkan.component.Device
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import org.lwjgl.vulkan.KHRAccelerationStructure


interface IAccelerationStructure {
    var vkHandle: Long
    var deviceAddress: Long
    var buffer: VulkanBuffer

    fun destroy(device: Device) {
        KHRAccelerationStructure.vkDestroyAccelerationStructureKHR(device.vkHandle, this.vkHandle, null)
        buffer.destroy()
    }
}