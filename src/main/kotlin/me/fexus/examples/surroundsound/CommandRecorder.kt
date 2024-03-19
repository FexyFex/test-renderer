package me.fexus.examples.surroundsound

import me.fexus.vulkan.component.CommandBuffer


interface CommandRecorder {
    fun recordRenderCommands(commandBuffer: CommandBuffer) {}
    fun recordComputeCommands(commandBuffer: CommandBuffer) {}
}