package me.fexus.window.input.packet

interface InputPacket {
    var isConsumed: Boolean

    fun consume() { isConsumed = true }
}