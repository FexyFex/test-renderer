package me.fexus.window.input.packet

import me.fexus.window.input.Key

data class KeyInputPacket(
    val keysDown: List<Key>,
    val keysJustPressed: List<Key>,
    val keysJustReleased: List<Key>,
): InputPacket {
    override var isConsumed: Boolean = false
}
