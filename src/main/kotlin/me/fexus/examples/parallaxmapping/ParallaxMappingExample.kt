package me.fexus.examples.parallaxmapping

import me.fexus.examples.RenderApplication
import me.fexus.vulkan.FramePreparation
import me.fexus.vulkan.FrameSubmitData
import me.fexus.vulkan.VulkanRendererBase
import me.fexus.window.Window


class ParallaxMappingExample: VulkanRendererBase(createWindow()), RenderApplication {
    fun start() {
        initVulkanCore()
        startRenderLoop(window, this)
    }

    override fun recordFrame(preparation: FramePreparation): FrameSubmitData {

        return FrameSubmitData(preparation.acquireSuccessful, preparation.imageIndex)
    }

    override fun destroy() {
        super.destroy()
    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ParallaxMappingExample().start()
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