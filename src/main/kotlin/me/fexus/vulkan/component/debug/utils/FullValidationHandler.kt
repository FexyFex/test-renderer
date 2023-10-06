package me.fexus.vulkan.component.debug.utils

import me.fexus.vulkan.component.debug.utils.ValidationReport.Label
import me.fexus.vulkan.component.debug.utils.VulkanMessageSeverity.*
import me.fexus.vulkan.component.debug.utils.VulkanMessageType.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*


class FullValidationHandler {
    fun handle(severity: VulkanMessageSeverity, types: Array<VulkanMessageType>, report: ValidationReport) {
        val start = when (severity) {
            VERBOSE -> "${AnsiColor.CYAN}Verbose"
            INFO    -> "${AnsiColor.GREEN}Info"
            WARNING -> "${AnsiColor.YELLOW}Warning"
            ERROR   -> "${AnsiColor.RED}Error"
        }

        val type = types.map {
            when (it) {
                GENERAL -> "${AnsiColor.CYAN}General${AnsiColor.CYAN}"
                VALIDATION  -> "${AnsiColor.GREEN}Validation${AnsiColor.CYAN}"
                PERFORMANCE -> "${AnsiColor.PURPLE}Performance${AnsiColor.CYAN}"
            }
        }

        val message = buildString {
            append(start).append(AnsiColor.CYAN).append(" | ")
            append(type.joinToString()).append(" | ")
            append("Message ID: ").append(report.messageIdNumber)
            report.messageIdName?.let { append(" ($it)") }
            append(" | ")
            append(report.message)

            val objects = report.objects
            if (!objects.isNullOrEmpty()) {
                appendln()

                val indent = " ".repeat(start.lengthWithoutColor() - 1)
                append(indent)
                appendln("${AnsiColor.PURPLE}> Associated Objects:${AnsiColor.CYAN}")

                val header = listOf(
                        "${AnsiColor.YELLOW}Type${AnsiColor.CYAN}",
                        "${AnsiColor.YELLOW}Handle${AnsiColor.CYAN}",
                        "${AnsiColor.YELLOW}Name${AnsiColor.CYAN}"
                )
                val body = objects.map { listOf(mapObjectType(it.type), it.handle.toString(), it.name ?: "-") }
                val tableIndent = " ".repeat(start.lengthWithoutColor() + 1)
                appendln(buildTable(listOf(header) + body).prependIndent(tableIndent))
            }

            fun appendLabels(title: String, labels: List<Label>) {
                appendln()

                val indent = " ".repeat(start.lengthWithoutColor() - 1)
                append(indent)
                appendln(title)

                val header = listOf(
                        "${AnsiColor.YELLOW}Name${AnsiColor.CYAN}",
                        "${AnsiColor.YELLOW}Color${AnsiColor.CYAN}"
                )
                val body = labels.map { listOf(it.name, it.color?.debugString() ?: "-") }
                val tableIndent = " ".repeat(start.lengthWithoutColor() + 1)
                appendln(buildTable(listOf(header) + body).prependIndent(tableIndent))
            }

            val queueLabels = report.queueLabels
            if (queueLabels != null && queueLabels.isNotEmpty()) {
                appendLabels("${AnsiColor.PURPLE}> Queue Labels:", queueLabels)
            }

            val commandBufferLabels = report.commandBufferLabels
            if (commandBufferLabels != null && commandBufferLabels.isNotEmpty()) {
                appendLabels("${AnsiColor.PURPLE}> Command Buffer Labels:", commandBufferLabels)
            }

            append(AnsiColor.RESET)
        }

        println(message)
    }

    private fun buildTable(rows: List<List<String>>): String {
        val height = rows.size
        val width = rows.map { it.size }.maxOrNull() ?: 0

        val rowStrings = Array(height) { "| " }

        repeat(width) { column ->
            rows.forEachIndexed { row, content ->
                val value = content.getOrNull(column) ?: ""
                rowStrings[row] += value
            }

            val maxLength = rowStrings.map { it.lengthWithoutColor() }.maxOrNull() ?: 0

            for ((index, rowString) in rowStrings.withIndex()) {
                val missing = maxLength - rowString.lengthWithoutColor()
                rowStrings[index] = rowString.padEnd(rowString.length + missing + 1, ' ')
            }

            repeat(rowStrings.size) {
                rowStrings[it] += "| "
            }
        }

        return buildString {
            rowStrings.forEach {
                appendln(it.trim())
            }
        }
    }

    private fun String.lengthWithoutColor(): Int {
        return this.length - (this.count { it == '\u001B' } * 5)
    }

    companion object {
        private val objectTypes = mapOf(
                VK_OBJECT_TYPE_UNKNOWN to "Unknown/Undefined Handle",
                VK_OBJECT_TYPE_INSTANCE to "VkInstance",
                VK_OBJECT_TYPE_PHYSICAL_DEVICE to "VkPhysicalDevice",
                VK_OBJECT_TYPE_DEVICE to "VkDevice",
                VK_OBJECT_TYPE_QUEUE to "VkQueue",
                VK_OBJECT_TYPE_SEMAPHORE to "VkSemaphore",
                VK_OBJECT_TYPE_COMMAND_BUFFER to "VkCommandBuffer",
                VK_OBJECT_TYPE_FENCE to "VkFence",
                VK_OBJECT_TYPE_DEVICE_MEMORY to "VkDeviceMemory",
                VK_OBJECT_TYPE_BUFFER to "VkBuffer",
                VK_OBJECT_TYPE_IMAGE to "VkImage",
                VK_OBJECT_TYPE_EVENT to "VkEvent",
                VK_OBJECT_TYPE_QUERY_POOL to "VkQueryPool",
                VK_OBJECT_TYPE_BUFFER_VIEW to "VkBufferView",
                VK_OBJECT_TYPE_IMAGE_VIEW to "VkImageView",
                VK_OBJECT_TYPE_SHADER_MODULE to "VkShaderModule",
                VK_OBJECT_TYPE_PIPELINE_CACHE to "VkPipelineCache",
                VK_OBJECT_TYPE_PIPELINE_LAYOUT to "VkPipelineLayout",
                VK_OBJECT_TYPE_RENDER_PASS to "VkRenderPass",
                VK_OBJECT_TYPE_PIPELINE to "VkPipeline",
                VK_OBJECT_TYPE_DESCRIPTOR_SET_LAYOUT to "VkDescriptorSetLayout",
                VK_OBJECT_TYPE_SAMPLER to "VkSampler",
                VK_OBJECT_TYPE_DESCRIPTOR_POOL to "VkDescriptorPool",
                VK_OBJECT_TYPE_DESCRIPTOR_SET to "VkDescriptorSet",
                VK_OBJECT_TYPE_FRAMEBUFFER to "VkFramebuffer",
                VK_OBJECT_TYPE_COMMAND_POOL to "VkCommandPool",
                VK11.VK_OBJECT_TYPE_SAMPLER_YCBCR_CONVERSION to "VkSamplerYcbcrConversion",
                VK11.VK_OBJECT_TYPE_DESCRIPTOR_UPDATE_TEMPLATE to "VkDescriptorUpdateTemplate",
                KHRSurface.VK_OBJECT_TYPE_SURFACE_KHR to "VkSurfaceKHR",
                KHRSwapchain.VK_OBJECT_TYPE_SWAPCHAIN_KHR to "VkSwapchainKHR",
                KHRDisplay.VK_OBJECT_TYPE_DISPLAY_KHR to "VkDisplayKHR",
                KHRDisplay.VK_OBJECT_TYPE_DISPLAY_MODE_KHR to "VkDisplayModeKHR",
                EXTDebugReport.VK_OBJECT_TYPE_DEBUG_REPORT_CALLBACK_EXT to "VkDebugReportCallbackEXT",
                EXTDebugUtils.VK_OBJECT_TYPE_DEBUG_UTILS_MESSENGER_EXT to "VkDebugUtilsMessengerEXT",
                EXTValidationCache.VK_OBJECT_TYPE_VALIDATION_CACHE_EXT to "VkValidationCacheEXT",
                NVRayTracing.VK_OBJECT_TYPE_ACCELERATION_STRUCTURE_NV to "VkAccelerationStructureNV",
                INTELPerformanceQuery.VK_OBJECT_TYPE_PERFORMANCE_CONFIGURATION_INTEL to "VkPerformanceConfigurationINTEL"
        )

        private fun mapObjectType(type: Int): String {
            return (objectTypes[type] ?: "Unknown") + " (ID: $type)"
        }
    }
}
