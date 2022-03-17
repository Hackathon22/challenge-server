package core


abstract class Event

abstract class ValueChangedEvent(val valueName: String, val value: Any) : Event()

/**
 * An event directly related to an entity.
 */
abstract class EntityEvent(val entity: Entity) : Event()

/**
 * Event called when an entity was hit
 */
class HitEvent(val duration: Float, entity: Entity) : EntityEvent(entity)

class DamageEvent(val damage: Float, entity: Entity, var fatal : Boolean = false) : EntityEvent(entity)

/**
 * A physical event related to a certain entity
 */
abstract class PhysicalEvent(entity: Entity) : EntityEvent(entity)

/**
 * Called when a collision is detected between two entities.
 */
class CollisionEvent(val collidedEntity: Entity, entity: Entity, val angle: Vec3F) : PhysicalEvent(entity)

abstract class InputEvent : Event()

data class WindowResizeEvent(val newSize: Vec2F) : ValueChangedEvent("windowSize", newSize)

interface IObserver {
    fun onEvent(event: Event, observable: IObservable, instance: Instance)
}

interface IObservable {
    val observers : ArrayList<IObserver>

    fun addObserver(observer: IObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: IObserver) {
        observers.remove(observer)
    }

    fun notifyObservers(event: Event, instance: Instance) {
        observers.forEach { it.onEvent(event, this, instance) }
    }

}