package me.fexus.window.input.packet

interface InputPacketReceiverIgnoreConsumption: InputPacketReceiver {
    fun onConsumedPacketReceived(inputPacket: InputPacket)
}