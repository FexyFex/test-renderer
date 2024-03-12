package me.fexus.audio

import me.fexus.audio.command.*
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
                is CommandSetListenerData -> {
                    this.library.listenerData = command.listenerData
                    command.result = true
                }
                is CommandSetListenerPosition -> {
                    this.library.setListenerPosition(command.position)
                    command.result = true
                }
                is CommandSetListenerVelocity -> {
                    this.library.setListenerVelocity(command.velocity)
                    command.result = true
                }
                is CommandSetListenerOrientation -> {
                    this.library.setListenerRotation(command.up, command.lookingAt)
                    command.result = true
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