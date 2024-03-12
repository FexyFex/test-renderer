package me.fexus.audio.command

import me.fexus.audio.AudioCommand
import me.fexus.audio.SoundEmitter
import java.util.concurrent.atomic.AtomicBoolean

class CommandCreateEmitter: AudioCommand<SoundEmitter> {
    override var isExecuted = AtomicBoolean(false)
    override lateinit var result: SoundEmitter
}