package core

import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

abstract class System(override val observers: ArrayList<IObserver> = ArrayList()) : IObserver, IObservable {
    // set containing unique values, with an importance in order
    protected var entities = LinkedHashSet<Entity>()

    protected var _initialized = false

    fun update(instance: Instance, delta: Float) {
        assert(_initialized)
        updateLogic(instance, delta)
    }

    fun initialize(vararg arg: Any) {
        assert(initializeLogic(*arg))
        _initialized = true
    }

    protected abstract fun initializeLogic(vararg arg : Any) : Boolean

    protected abstract fun updateLogic(instance: Instance, delta: Float)

    protected abstract fun onEntityAdded(entity: Entity)

    protected abstract fun onEntityRemoved(entity: Entity)

    fun addEntity(entity: Entity) {
        entities.add(entity)
        onEntityAdded(entity)
    }

    fun removeEntity(entity: Entity) {
        entities.remove(entity)
        onEntityRemoved(entity)
    }

    override fun onEvent(event: Event, observable: IObservable) {
        // Does nothing by default, needs to be overwritten
    }
}

class SystemManager {
    internal val _systemMap = HashMap<KClass<out System>, System>()

    internal val _signatureMap = HashMap<KClass<out System>, Signature>()

    internal inline fun <reified T : System> registerSystem() : System {
        // checks if the system has not been initialized yet
        assert(_systemMap[T::class] == null)
        _systemMap[T::class] = T::class.createInstance()
        return _systemMap[T::class]!!
    }

    internal inline fun <reified T : System> setSignature(signature: Signature) {
        assert(_systemMap[T::class] != null)
        _signatureMap[T::class] = signature
    }

    internal inline fun <reified T: System> getSystem() : T {
        assert(_systemMap[T::class] != null)
        return (_systemMap[T::class] as T?)!!
    }

    fun entityDestroyed(entity: Entity) {
        _systemMap.forEach { _, v -> v.removeEntity(entity) }
    }

    fun entitySignatureChanged(entity: Entity, signature: Signature) {
        for ((k, v) in _systemMap) {
            if (_signatureMap[k] != null) {
                val systemSignature = _signatureMap[k]!!
                val entitySignature = signature.clone() as Signature
                entitySignature.and(systemSignature)
                if (entitySignature == systemSignature) {
                    v.addEntity(entity)
                    continue
                }
            }
            v.removeEntity(entity)
        }
    }
}
