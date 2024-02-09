package me.fexus.vulkan.descriptors.image

import me.fexus.vulkan.util.ImageExtent3D
import me.fexus.vulkan.descriptors.image.aspect.IImageAspect
import me.fexus.vulkan.descriptors.image.usage.IImageUsage
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlags
import org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE


data class VulkanImageConfiguration(
    val imageType: ImageType,
    val imageViewType: ImageViewType,
    val extent: ImageExtent3D,
    val mipLevels: Int,
    val sampleCount: Int,
    val arrayLayerCount: Int,
    val colorFormat: ImageColorFormat,
    val imageTiling: ImageTiling,
    val imageAspect: IImageAspect,
    val imageUsage: IImageUsage,
    val memoryProperties: MemoryPropertyFlags,
    val sharingMode: Int = VK_SHARING_MODE_EXCLUSIVE
)