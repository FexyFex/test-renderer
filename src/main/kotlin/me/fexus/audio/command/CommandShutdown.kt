package me.fexus.audio.command

import me.fexus.audio.AudioCommand
import java.util.concurrent.atomic.AtomicBoolean

class CommandShutdown: AudioCommand<Nothing> {
    override var isExecuted = AtomicBoolean(false)
    override lateinit var result: Nothing
}