package me.fexus.examples

import me.fexus.vulkan.VulkanRendererBase
import me.fexus.vulkan.util.FramePreparation
import me.fexus.window.Window
import kotlin.system.measureNanoTime


interface RenderApplication {
    fun startRenderLoop(window: Window, renderer: VulkanRendererBase) {
        val desiredFPS = 200
        val unlimitedFPS = true

        val optimalTime: Double = 1.0 / desiredFPS

        var time = 0f
        var frameDelta: Float
        var frameCounter = 0
        var lastFpsTime = 0.0
        var lastLoopTime: Double = System.nanoTime() / 1_000_000_000.0

        while (!window.shouldClose) {
            // Frames calculation logic
            //------------------------------------------------------------------------------------------
            val nowD: Double = System.nanoTime() / 1_000_000_000.0
            val updateLength: Float = (nowD - lastLoopTime).toFloat()
            time += updateLength
            frameDelta = (updateLength / optimalTime).toFloat()
            lastLoopTime = nowD
            val frameStart = System.nanoTime()
            lastFpsTime += updateLength
            frameCounter++
            if (lastFpsTime >= 1) {
                window.setTitleRememberFormer("${window.title}; current FPS: $frameCounter")
                lastFpsTime = 0.0
                frameCounter = 0
            }
            //------------------------------------------------------------------------------------------
            // Frames calculation logic


            // Update logic
            //------------------------------------------------------------------------------------------+
            val prep: FramePreparation = renderer.prepareFrame()
            if (prep.acquireSuccessful) {
                val frameData = renderer.recordFrame(prep, updateLength)
                if (frameData.doSubmit)
                    renderer.submitFrame(frameData)
            }

            window.pollEvents()
            //val totalTime = backendProcessingTime + windowProcessingTime
            //println("Processing times: game: $gameProcessingTime, backend: $backendProcessingTime, window: $windowProcessingTime, total: $totalTime")
            //------------------------------------------------------------------------------------------
            // Update logic


            // Frame limiting
            //------------------------------------------------------------------------------------------
            if (unlimitedFPS) continue

            val nanosPerFrame = 1_000_000_000 / desiredFPS
            val frameEnd = frameStart + nanosPerFrame
            while (System.nanoTime() < frameEnd) {
                Thread.sleep(1)
            }
            //------------------------------------------------------------------------------------------
            // Frame limiting
        }

        renderer.destroy()
        window.destroy()
    }
}