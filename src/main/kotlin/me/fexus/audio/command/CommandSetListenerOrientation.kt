package me.fexus.audio.command

import me.fexus.audio.AudioCommand
import me.fexus.audio.ListenerData
import me.fexus.math.vec.Vec3
import java.util.concurrent.atomic.AtomicBoolean


data class CommandSetListenerOrientation(val lookingAt: Vec3, val up: Vec3): AudioCommand<Boolean> {
    override var isExecuted = AtomicBoolean(false)
    override var result: Boolean = false
}