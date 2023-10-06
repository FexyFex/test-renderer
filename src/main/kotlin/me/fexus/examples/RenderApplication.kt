package me.fexus.examples

import me.fexus.vulkan.VulkanRendererBase
import me.fexus.window.Window


interface RenderApplication {
    fun startRenderLoop(window: Window, renderer: VulkanRendererBase) {
        val desiredFPS = 60

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
                window.setTitle("TestRenderer; current FPS: $frameCounter")
                lastFpsTime = 0.0
                frameCounter = 0
            }
            //------------------------------------------------------------------------------------------
            // Frames calculation logic


            // Update logic
            //------------------------------------------------------------------------------------------+

            renderer.prepareFrame()
            renderer.drawFrame()
            renderer.submitFrame()

            window.pollEvents()
            //val totalTime = backendProcessingTime + windowProcessingTime
            //println("Processing times: game: $gameProcessingTime, backend: $backendProcessingTime, window: $windowProcessingTime, total: $totalTime")
            //------------------------------------------------------------------------------------------
            // Update logic


            // Frame limiting
            //------------------------------------------------------------------------------------------
            //if (!AppInfo.cappedFPS) continue

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