package me.fexus.examples.coolvoxelrendering.event

// A child of EventPoster can be subscribed to by a child of Subscriber
// meaning the Subscriber will recieve all event updates of the EventPoster
interface EventPoster {
    val subscribers: MutableList<EventSubscriber>

    fun subscribe(subscriber: EventSubscriber) = subscribers.add(subscriber)
    fun unsubscribe(subscriber: EventSubscriber) = subscribers.remove(subscriber)
    fun publishEventToSubscribers(event: IEvent) = subscribers.forEach { it.publish(event) }
}