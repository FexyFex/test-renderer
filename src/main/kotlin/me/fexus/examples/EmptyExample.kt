package me.fexus.examples

import me.fexus.vulkan.VulkanRendererBase
import me.fexus.vulkan.util.FramePreparation
import me.fexus.vulkan.util.FrameSubmitData
import me.fexus.vulkan.util.ImageExtent2D
import me.fexus.window.Window


class EmptyExample: VulkanRendererBase(createWindow()) {
    fun start() {
        initVulkanCore()
        // init objects such as buffers and images here
        startRenderLoop(window, this)
    }

    override fun recordFrame(preparation: FramePreparation): FrameSubmitData {
        if (!preparation.acquireSuccessful) return FrameSubmitData(false, preparation.imageIndex)
        return FrameSubmitData(true, preparation.imageIndex)
    }

    override fun onResizeDestroy() {

    }

    override fun onResizeRecreate(newExtent2D: ImageExtent2D) {

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            EmptyExample().start()
        }

        private fun createWindow() = Window("Parallax Mapping") {
            windowVisible()
            enableResizable()
            setInitialWindowSize(1067,600)
            enableDecoration()
            setInitialWindowPosition(400, 300)
            enableAutoIconify()
        }
    }
}