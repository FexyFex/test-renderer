package me.fexus.vulkan

import me.fexus.window.Window


class VulkanRenderer(private val window: Window) {
    private val core = VulkanCore(window)
    private val surface = Surface()


    fun init(): VulkanRenderer {
        core.createInstance()
        surface.create(core.instance, window)
        core.init()
        return this
    }


    fun drawFrame() {

    }
}