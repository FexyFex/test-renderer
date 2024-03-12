package me.fexus.audio.command

import me.fexus.audio.AudioCommand
import me.fexus.audio.ListenerData
import java.util.concurrent.atomic.AtomicBoolean


data class CommandSetListenerData(val listenerData: ListenerData): AudioCommand<Boolean> {
    override var isExecuted = AtomicBoolean(false)
    override var result: Boolean = false
}