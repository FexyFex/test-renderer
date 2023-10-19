package me.fexus.vulkan.util

import me.fexus.vulkan.component.CommandBuffer
import me.fexus.vulkan.component.pipeline.pipelinestage.IPipelineStage
import me.fexus.vulkan.component.Semaphore


data class FrameSubmitData(
    val doSubmit: Boolean,
    val imageIndex: Int,
    val additionalWaitSemaphores: List<Semaphore> = emptyList(),
    val additionalCommandBuffers: List<CommandBuffer> = emptyList(),
    val additionalSignalSemaphores: List<Semaphore> = emptyList(),
    val additionalWaitStages: List<IPipelineStage> = emptyList()
)