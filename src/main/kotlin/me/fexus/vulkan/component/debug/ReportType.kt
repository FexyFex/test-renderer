package me.fexus.vulkan.component.debug

import org.lwjgl.vulkan.EXTDebugReport

enum class ReportType(override val mask: Int) : Flag {
    INFORMATION(EXTDebugReport.VK_DEBUG_REPORT_INFORMATION_BIT_EXT),
    WARNING(EXTDebugReport.VK_DEBUG_REPORT_WARNING_BIT_EXT),
    PERFORMANCE_WARNING(EXTDebugReport.VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT),
    ERROR(EXTDebugReport.VK_DEBUG_REPORT_ERROR_BIT_EXT),
    DEBUG(EXTDebugReport.VK_DEBUG_REPORT_DEBUG_BIT_EXT),
    ALL(combined(INFORMATION, WARNING, PERFORMANCE_WARNING, ERROR, DEBUG));
}
