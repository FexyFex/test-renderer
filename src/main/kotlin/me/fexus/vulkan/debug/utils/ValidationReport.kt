package me.fexus.vulkan.debug.utils

import me.fexus.vulkan.debug.utils.AnsiColor.*
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VkDebugUtilsLabelEXT
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT
import org.lwjgl.vulkan.VkDebugUtilsObjectNameInfoEXT


data class ValidationReport(
    val messageIdName: String?,
    val messageIdNumber: Int,
    val message: String,
    val queueLabels: List<Label>?,
    val commandBufferLabels: List<Label>?,
    val objects: List<ObjectNameInfo>?
) {
    companion object {
        fun from(data: VkDebugUtilsMessengerCallbackDataEXT): ValidationReport {
            val messageIdName = data.pMessageIdName()?.let { MemoryUtil.memUTF8(it) }
            val messageIdNumber = data.messageIdNumber()
            val message = MemoryUtil.memUTF8(data.pMessage())

            val queueLabelCount = data.queueLabelCount()
            val queueLabels = data.pQueueLabels()?.let { labels ->
                Array(queueLabelCount) {
                    Label.from(labels[it])
                }
            }

            val commandBufferLabelCount = data.cmdBufLabelCount()
            val commandBufferLabels = data.pCmdBufLabels()?.let { labels ->
                Array(commandBufferLabelCount) {
                    Label.from(labels[it])
                }
            }

            val objectCount = data.objectCount()
            val objects = data.pObjects()?.let { objects ->
                Array(objectCount) {
                    ObjectNameInfo.from(objects[it])
                }
            }

            return ValidationReport(
                    messageIdName,
                    messageIdNumber,
                    message,
                    queueLabels?.toList(),
                    commandBufferLabels?.toList(),
                    objects?.toList()
            )
        }
    }

    data class ObjectNameInfo(val type: Int, val handle: Long, val name: String?) {
        companion object {
            fun from(objectNameInfo: VkDebugUtilsObjectNameInfoEXT): ObjectNameInfo {
                val objectType = objectNameInfo.objectType()
                val objectHandle = objectNameInfo.objectHandle()
                val objectName = objectNameInfo.pObjectNameString()

                return ObjectNameInfo(objectType, objectHandle, objectName)
            }
        }
    }

    data class Label(val name: String, val color: LabelColor?) {
        companion object {
            fun from(label: VkDebugUtilsLabelEXT): Label {
                val colorValues = label.color()

                val name = label.pLabelNameString()

                val color =
                        if (arrayOf(colorValues[0], colorValues[1], colorValues[2], colorValues[3]).any { it != 0f }) {
                            LabelColor(colorValues[0], colorValues[1], colorValues[2], colorValues[3])
                        } else {
                            null
                        }

                return Label(name, color)
            }
        }
    }

    data class LabelColor(val red: Float, val green: Float, val blue: Float, val alpha: Float) {
        fun debugString(): String {
            return "($RED$red$CYAN | $GREEN$green$CYAN | $BLUE$blue$CYAN | $WHITE$alpha$CYAN)"
        }
    }
}
