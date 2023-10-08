package me.fexus.vulkan.descriptors.image

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.component.Device
import me.fexus.vulkan.component.PhysicalDevice
import me.fexus.vulkan.descriptors.DescriptorFactory
import me.fexus.vulkan.descriptors.image.sampler.VulkanSampler
import me.fexus.vulkan.descriptors.image.sampler.VulkanSamplerLayout
import me.fexus.vulkan.exception.catchVK
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*


class VulkanImageFactory: DescriptorFactory {
    override lateinit var physicalDevice: PhysicalDevice
    override lateinit var device: Device

    fun init(physicalDevice: PhysicalDevice, device: Device) {
        this.physicalDevice = physicalDevice
        this.device = device
    }


    /**
     * Attempts to create a VulkanImage according to the given preferred VulkanImageLayout.
     * If it is impossible to create the VulkanImage with the given VulkanImageLayout,
     * the function returns a VulkanImage with an altered VulkanImageLayout to indicate
     * the changes that were made during creation.
     */
    fun createImage(preferredLayout: VulkanImageLayout): VulkanImage {
        val image = runMemorySafe {
            val imageInfo = calloc<VkImageCreateInfo>() {
                sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                pNext(0)
                imageType(preferredLayout.imageType.vkValue)
                mipLevels(preferredLayout.mipLevels)
                arrayLayers(preferredLayout.arrayLayerCount)
                format(preferredLayout.colorFormat.vkValue)
                tiling(preferredLayout.imageTiling.vkValue)
                initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                usage(preferredLayout.imageUsage.vkBits)
                samples(preferredLayout.sampleCount)
                sharingMode(preferredLayout.sharingMode)
                extent()
                    .width(preferredLayout.extent.width)
                    .height(preferredLayout.extent.height)
                    .depth(preferredLayout.extent.depth)
            }

            val pImageHandle = allocateLong(1)
            vkCreateImage(device.vkHandle, imageInfo, null, pImageHandle).catchVK()
            val imageHandle = pImageHandle[0]

            val memRequirements = calloc<VkMemoryRequirements>()
            vkGetImageMemoryRequirements(device.vkHandle, imageHandle, memRequirements)
            val memoryTypeIndex = findMemoryTypeIndex(memRequirements.memoryTypeBits(), preferredLayout.memoryProperties)

            val allocInfo = calloc<VkMemoryAllocateInfo>() {
                sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                pNext(0)
                allocationSize(memRequirements.size())
                memoryTypeIndex(memoryTypeIndex)
            }

            val pImageMemoryHandle = allocateLong(1)
            vkAllocateMemory(device.vkHandle, allocInfo, null, pImageMemoryHandle)
            val imageMemoryHandle = pImageMemoryHandle[0]

            vkBindImageMemory(device.vkHandle, imageHandle, imageMemoryHandle, 0)

            val viewInfo = calloc<VkImageViewCreateInfo>() {
                sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                pNext(0)
                image(imageHandle)
                viewType(preferredLayout.imageViewType.vkValue)
                format(preferredLayout.colorFormat.vkValue)
                subresourceRange()
                    .aspectMask(preferredLayout.imageAspect.vkBits)
                    .baseMipLevel(0)
                    .levelCount(preferredLayout.mipLevels)
                    .baseArrayLayer(0)
                    .layerCount(preferredLayout.arrayLayerCount)
            }

            val pImageViewHandle = allocateLong(1)
            vkCreateImageView(device.vkHandle, viewInfo, null, pImageViewHandle)
            val imageViewHandle = pImageViewHandle[0]

            val actualLayout = VulkanImageLayout(
                preferredLayout.imageType,
                preferredLayout.imageViewType,
                preferredLayout.extent,
                preferredLayout.mipLevels,
                preferredLayout.sampleCount,
                preferredLayout.arrayLayerCount,
                preferredLayout.colorFormat,
                preferredLayout.imageTiling,
                preferredLayout.imageAspect,
                preferredLayout.imageUsage,
                preferredLayout.memoryProperties,
                preferredLayout.sharingMode,
            )

            return@runMemorySafe VulkanImage(device, imageHandle, imageMemoryHandle, imageViewHandle, actualLayout)
        }
        return image
    }

    fun createSampler(layout: VulkanSamplerLayout): VulkanSampler = runMemorySafe{
        val createInfo = calloc<VkSamplerCreateInfo>() {
            sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
            pNext(0)
            flags(0)
            minFilter(layout.filtering.vkValue)
            magFilter(layout.filtering.vkValue)
            addressModeU(layout.addressMode.vkValue)
            addressModeV(layout.addressMode.vkValue)
            addressModeW(layout.addressMode.vkValue)
            anisotropyEnable(false)
            maxAnisotropy(0f)
            borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
            unnormalizedCoordinates(false)
            compareEnable(false)
            compareOp(VK_COMPARE_OP_ALWAYS)
            mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
            minLod(0f)
            maxLod(layout.mipLevels.toFloat())
            mipLodBias(0f)
        }

        val pSamplerHandle = allocateLong(1)
        vkCreateSampler(device.vkHandle, createInfo, null, pSamplerHandle)

        return@runMemorySafe VulkanSampler(pSamplerHandle[0], layout)
    }
}