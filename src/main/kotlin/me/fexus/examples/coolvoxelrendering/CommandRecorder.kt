package me.fexus.examples.coolvoxelrendering

import me.fexus.vulkan.component.CommandBuffer

interface CommandRecorder {
    fun recordComputeCommands(commandBuffer: CommandBuffer, frameIndex: Int) {}
    fun recordGraphicsCommands(commandBuffer: CommandBuffer, frameIndex: Int) {}
}