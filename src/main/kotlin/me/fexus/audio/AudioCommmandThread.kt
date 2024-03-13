package me.fexus.audio

import me.fexus.audio.command.*
import me.fexus.math.vec.Vec3
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean


// This thread will oversee all the emitters and update them periodically
class AudioCommmandThread: Thread() {
    private lateinit var library: AudioLibrary
    private val keepRunning = AtomicBoolean(false)

    private val commands = ConcurrentLinkedQueue<AudioCommand<*>>()

    private val emitters = mutableListOf<SoundEmitter>()


    override fun run() {
        super.run()

        keepRunning.set(true)

        while (keepRunning.get()) {
            processCommands()
            emitters.forEach(SoundEmitter::_process)
        }

        shutdown()
    }


    fun setListenerData(listenerData: ListenerData) { library.listenerData = listenerData }
    fun setListenerPosition(position: Vec3) = library.setListenerPosition(position)
    fun setListenerOrientation(lookAt: Vec3, up: Vec3) = library.setListenerOrientation(lookAt, up)
    fun setListenerVelocity(velocity: Vec3) = library.setListenerVelocity(velocity)

    fun submitCommand(command: AudioCommand<*>) {
        commands.add(command)
    }

    private fun processCommands() {
        while (commands.isNotEmpty()) {
            val command = commands.poll()

            when (command) {
                is CommandInitializeLibrary -> {
                    val library = command.library.constructors.first().call()
                    if (library is AudioLibrary) {
                        library.init()
                        this.library = library
                        command.result = true
                    } else command.result = false
                }
                is CommandCreateEmitter -> {
                    val emitter = library.createEmitter()
                    this.emitters.add(emitter)
                    command.result = emitter
                }
                is CommandShutdown -> {
                    keepRunning.set(false)
                }
            }

            command.isExecuted.set(true)
        }
    }

    private fun shutdown() {
        this.library.shutdown()
        emitters.clear()
    }
}