package core

enum class EventType {
    VALUE_CHANGED
}

abstract class Event {
    abstract fun getEventType() : EventType
}

abstract class ValueChangedEvent(val valueName: String, val value: Any) : Event() {
    override fun getEventType(): EventType {
        return EventType.VALUE_CHANGED
    }
}

data class WindowResizeEvent(val newSize: Vec2F) : ValueChangedEvent("windowSize", newSize)

interface IObserver {
    fun onEvent(event: Event, observable: IObservable)
}

interface IObservable {
    val observers : ArrayList<IObserver>

    fun addObserver(observer: IObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: IObserver) {
        observers.remove(observer)
    }

    fun notifyObservers(event: Event) {
        observers.forEach { it.onEvent(event, this) }
    }

}