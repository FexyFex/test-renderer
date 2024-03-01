package me.fexus.window.input.packet


class InputPacketPipeline {
    private val hierarchy = InputReceiverHierarchy()


    fun addSubscriberWithPriority(sub: InputPacketReceiver, priority: Int) {
        hierarchy.addWithPriority(sub, priority)
    }

    fun removeSubscriber(sub: InputPacketReceiver) {
        hierarchy.remove(sub)
    }

    fun publishInputPacket(packet: InputPacket) {
        for (receiver in hierarchy) {
            if (!packet.isConsumed) {
                receiver.onPacketReceived(packet)
            } else {
                if (receiver is InputPacketReceiverIgnoreConsumption)
                    receiver.onConsumedPacketReceived(packet)
            }
        }
    }
}