package me.fexus.vulkan.descriptors.image

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.component.Device
import me.fexus.vulkan.component.PhysicalDevice
import me.fexus.vulkan.descriptors.DescriptorFactory
import me.fexus.vulkan.descriptors.image.sampler.VulkanSampler
import me.fexus.vulkan.descriptors.image.sampler.VulkanSamplerConfiguration
import me.fexus.vulkan.descriptors.memorypropertyflags.CombinedMemoryPropertyFlags
import me.fexus.vulkan.exception.catchVK
import me.fexus.vulkan.memory.MemoryStatistics
import me.fexus.vulkan.memory.budget.MemoryHeapTypeFinder
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*


class VulkanImageFactory: DescriptorFactory {
    override lateinit var memoryStatistics: MemoryStatistics
    override lateinit var memoryFinder: MemoryHeapTypeFinder
    override lateinit var physicalDevice: PhysicalDevice
    override lateinit var device: Device

    fun create(memoryStatistics: MemoryStatistics, memoryValidator: MemoryHeapTypeFinder, physicalDevice: PhysicalDevice, device: Device) {
        this.memoryStatistics = memoryStatistics
        this.memoryFinder = memoryValidator
        this.physicalDevice = physicalDevice
        this.device = device
    }


    /**
     * Attempts to create a VulkanImage according to the given preferred VulkanImageConfiguration.
     * If it is impossible to create the VulkanImage with the given VulkanImageConfiguration,
     * the function returns a VulkanImage with an altered VulkanImageConfiguration to indicate
     * the changes that were made during creation.
     */
    fun createImage(preferredConfig: VulkanImageConfiguration): VulkanImage {
        val image = runMemorySafe {
            //val formatPorperties = calloc(VkFormatProperties2::calloc)
            //vkGetPhysicalDeviceFormatProperties2(physicalDevice.vkHandle, preferredConfig.colorFormat.vkValue, formatPorperties)

            val imageInfo = calloc(VkImageCreateInfo::calloc) {
                sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                pNext(0)
                imageType(preferredConfig.imageType.vkValue)
                mipLevels(preferredConfig.mipLevels)
                arrayLayers(preferredConfig.arrayLayerCount)
                format(preferredConfig.colorFormat.vkValue)
                tiling(preferredConfig.imageTiling.vkValue)
                initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                usage(preferredConfig.imageUsage.vkBits)
                samples(preferredConfig.sampleCount)
                sharingMode(preferredConfig.sharingMode)
                extent()
                    .width(preferredConfig.extent.width)
                    .height(preferredConfig.extent.height)
                    .depth(preferredConfig.extent.depth)
            }

            val pImageHandle = allocateLong(1)
            vkCreateImage(device.vkHandle, imageInfo, null, pImageHandle).catchVK()
            val imageHandle = pImageHandle[0]

            val memRequirements = calloc(VkMemoryRequirements::calloc)
            vkGetImageMemoryRequirements(device.vkHandle, imageHandle, memRequirements)

            val imageSize = preferredConfig.extent.width * preferredConfig.extent.height * preferredConfig.extent.depth * 4L

            var searchReport = memoryFinder.findMemoryType(
                imageSize,
                CombinedMemoryPropertyFlags(memRequirements.memoryTypeBits()),
                preferredConfig.memoryPropertyFlags
            )

            // Keep looking for a heap until we find one with a sufficient memory budget
            while (!searchReport.heapBudgetSufficient && !searchReport.noMoreMemoryAvailable) {
                searchReport = searchReport.suggestAlternative()
            }

            // TODO: return null (or some sort of error info object) when allocation fails completely
            if (searchReport.noMoreMemoryAvailable)
                println("MEEP")
            //return@runMemorySafe null

            val allocInfo = calloc(VkMemoryAllocateInfo::calloc) {
                sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                pNext(0)
                allocationSize(memRequirements.size())
                memoryTypeIndex(searchReport.type.index)
            }

            val pImageMemoryHandle = allocateLong(1)
            vkAllocateMemory(device.vkHandle, allocInfo, null, pImageMemoryHandle).catchVK()
            val imageMemoryHandle = pImageMemoryHandle[0]

            vkBindImageMemory(device.vkHandle, imageHandle, imageMemoryHandle, 0).catchVK()

            val viewInfo = calloc(VkImageViewCreateInfo::calloc) {
                sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                pNext(0)
                image(imageHandle)
                viewType(preferredConfig.imageViewType.vkValue)
                format(preferredConfig.colorFormat.vkValue)
                subresourceRange()
                    .aspectMask(preferredConfig.imageAspect.vkBits)
                    .baseMipLevel(0)
                    .levelCount(preferredConfig.mipLevels)
                    .baseArrayLayer(0)
                    .layerCount(preferredConfig.arrayLayerCount)
            }

            val pImageViewHandle = allocateLong(1)
            vkCreateImageView(device.vkHandle, viewInfo, null, pImageViewHandle).catchVK()
            val imageViewHandle = pImageViewHandle[0]

            val actualConfig = VulkanImageConfiguration(
                preferredConfig.imageType,
                preferredConfig.imageViewType,
                preferredConfig.extent,
                preferredConfig.mipLevels,
                preferredConfig.sampleCount,
                preferredConfig.arrayLayerCount,
                preferredConfig.colorFormat,
                preferredConfig.imageTiling,
                preferredConfig.imageAspect,
                preferredConfig.imageUsage,
                searchReport.type.memoryPropertyFlags,
                preferredConfig.formatFeatureFlags,
                preferredConfig.sharingMode,
            )

            return@runMemorySafe VulkanImage(device, imageHandle, imageMemoryHandle, imageViewHandle, actualConfig)
        }
        return image
    }

    fun createSampler(samplerConfig: VulkanSamplerConfiguration): VulkanSampler = runMemorySafe{
        val createInfo = calloc(VkSamplerCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
            pNext(0)
            flags(0)
            minFilter(samplerConfig.filtering.vkValue)
            magFilter(samplerConfig.filtering.vkValue)
            addressModeU(samplerConfig.addressMode.vkValue)
            addressModeV(samplerConfig.addressMode.vkValue)
            addressModeW(samplerConfig.addressMode.vkValue)
            anisotropyEnable(false)
            maxAnisotropy(0f)
            borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
            unnormalizedCoordinates(false)
            compareEnable(false)
            compareOp(VK_COMPARE_OP_ALWAYS)
            mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
            minLod(0f)
            maxLod(samplerConfig.mipLevels.toFloat())
            mipLodBias(0f)
        }

        val pSamplerHandle = allocateLong(1)
        vkCreateSampler(device.vkHandle, createInfo, null, pSamplerHandle)

        return@runMemorySafe VulkanSampler(device, pSamplerHandle[0], samplerConfig)
    }
}