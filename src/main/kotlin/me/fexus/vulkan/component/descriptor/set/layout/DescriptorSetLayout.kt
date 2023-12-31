package me.fexus.vulkan.component.descriptor.set.layout

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.component.Device
import me.fexus.vulkan.exception.catchVK
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBindingFlagsCreateInfo
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo


class DescriptorSetLayout {
    var vkHandle: Long = 0L; private set

    fun create(device: Device, plan: DescriptorSetLayoutPlan) = runMemorySafe {
        val layoutBindings = calloc(VkDescriptorSetLayoutBinding::calloc, plan.bindings.size)
        plan.bindings.forEachIndexed { index, binding ->
            layoutBindings[index]
                .binding(binding.dstBinding)
                .descriptorType(binding.descriptorType.vkValue)
                .descriptorCount(binding.descriptorCount)
                .stageFlags(binding.shaderStage.vkBits)
                .pImmutableSamplers(null)
        }

        val pBindingFlags = allocateInt(plan.bindings.size)
        plan.bindings.forEachIndexed { index, binding ->
            pBindingFlags.put(index, binding.flags.vkBits)
        }

        val bindingFlags = calloc(VkDescriptorSetLayoutBindingFlagsCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO)
            pNext(0)
            bindingCount(plan.bindings.size)
            pBindingFlags(pBindingFlags)
        }

        val layoutCreateInfo = calloc(VkDescriptorSetLayoutCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
            pNext(bindingFlags.address())
            pBindings(layoutBindings)
            flags(plan.layoutFlags.vkBits)
        }

        val pDescSetLayoutHandle = allocateLong(1)
        vkCreateDescriptorSetLayout(device.vkHandle, layoutCreateInfo, null, pDescSetLayoutHandle).catchVK()
        this@DescriptorSetLayout.vkHandle = pDescSetLayoutHandle[0]

        return@runMemorySafe this@DescriptorSetLayout
    }

    fun destroy(device: Device) {
        vkDestroyDescriptorSetLayout(device.vkHandle, vkHandle, null)
    }
}