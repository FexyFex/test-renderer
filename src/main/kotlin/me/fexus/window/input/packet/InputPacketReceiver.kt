package me.fexus.window.input.packet

interface InputPacketReceiver {
    fun onPacketReceived(packet: InputPacket)
}