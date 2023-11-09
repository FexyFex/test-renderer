package me.fexus.examples.hardwarevoxelraytracing.voxelanimation

import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.model.AnimatedBlobModel


fun main() {
    val blob = AnimatedBlobModel()

    var previousLoopTime = System.nanoTime() / ONE_BILLION
    while (true) {
        val currentLoopTime = System.nanoTime() / ONE_BILLION
        val timePassed = currentLoopTime - previousLoopTime
        previousLoopTime = currentLoopTime

        blob.tick(timePassed.toFloat())

        Thread.sleep(15)
    }
}

const val ONE_BILLION: Double = 1_000_000_000.0