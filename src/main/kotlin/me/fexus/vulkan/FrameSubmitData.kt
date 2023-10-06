package me.fexus.vulkan

import me.fexus.vulkan.pipeline.stage.IPipelineStage
import me.fexus.vulkan.sync.Semaphore


data class FrameSubmitData(
    val doSubmit: Boolean,
    val imageIndex: Int,
    val additionalWaitSemaphores: List<Semaphore> = emptyList(),
    val additionalCommandBuffers: List<CommandBuffer> = emptyList(),
    val additionalSignalSemaphores: List<Semaphore> = emptyList(),
    val additionalWaitStages: List<IPipelineStage> = emptyList()
)