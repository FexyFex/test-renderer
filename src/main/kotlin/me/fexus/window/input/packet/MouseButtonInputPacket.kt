package me.fexus.window.input.packet

import me.fexus.window.input.MouseButton

class MouseButtonInputPacket(
    val buttonsDown: List<MouseButton>,
    val buttonsJustPressed: List<MouseButton>,
    val buttonsJustReleased: List<MouseButton>
): InputPacket {
    override var isConsumed: Boolean = false
}