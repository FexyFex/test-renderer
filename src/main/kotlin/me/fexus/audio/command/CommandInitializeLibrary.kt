package me.fexus.audio.command

import me.fexus.audio.AudioCommand
import me.fexus.audio.AudioLibrary
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass


// Some libraries like OpenAL will be Thread-local, so it needs to be initialized and then used on the same thread
class CommandInitializeLibrary(val library: KClass<*>): AudioCommand<Boolean> {
    override var isExecuted = AtomicBoolean(false)
    override var result: Boolean = false
}