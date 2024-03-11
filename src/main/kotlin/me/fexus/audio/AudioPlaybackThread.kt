package me.fexus.audio

import java.util.concurrent.atomic.AtomicBoolean


// This thread will oversee all the channels and feed them data periodically
class AudioPlaybackThread(private val keepRunning: AtomicBoolean): Thread() {
    override fun run() {
        super.run()

        while (keepRunning.get()) {
            println(keepRunning.get())
            sleep(100)
        }
    }
}