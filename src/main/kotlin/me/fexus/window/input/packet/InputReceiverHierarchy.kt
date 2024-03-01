package me.fexus.window.input.packet

class InputReceiverHierarchy: Collection<InputPacketReceiver> {
    private var receivers = mutableListOf<ReceiverPriority>()
    override val size: Int = receivers.size

    fun addWithPriority(receiver: InputPacketReceiver, priority: Int) {
        val slot = receivers.indexOfFirst { it.priority >= priority }

        val newReceiverWithPriority = ReceiverPriority(receiver, priority)
        receivers.add(slot, newReceiverWithPriority)
    }

    fun remove(receiver: InputPacketReceiver) = receivers.removeIf { it.receiver == receiver }

    override fun isEmpty(): Boolean = receivers.isEmpty()
    override fun contains(element: InputPacketReceiver): Boolean = receivers.any { it.receiver == element }
    override fun containsAll(elements: Collection<InputPacketReceiver>) = elements.all { this.contains(it) }
    override fun iterator(): Iterator<InputPacketReceiver> = receivers.map { it.receiver }.iterator()

    private data class ReceiverPriority(val receiver: InputPacketReceiver, val priority: Int)
}