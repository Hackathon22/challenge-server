package core

import components.DynamicComponent
import components.TransformComponent
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass

typealias Index = Int
typealias ComponentType = Int

const val maxComponentTypes =  32
typealias Signature = BitSet

interface IComponent {
}

interface IComponentArray {
    fun entityDestroyed(entity: Entity)
}

class ComponentArray<T : IComponent> : IComponentArray {

    private val _componentArray = ArrayList<T>()

    private val _entityToIndex = HashMap<Entity, Index>()

    private val _indexToEntity = HashMap<Index, Entity>()

    private var _indexCounter = 0

    fun insertData(entity: Entity, component: IComponent) {
        assert(_entityToIndex[entity] == null)

        val newIndex = _indexCounter
        _entityToIndex[entity] = newIndex
        _indexToEntity[newIndex] = entity

        _componentArray.add(component as T)

        _indexCounter++
    }

    fun removeData(entity: Entity) {
        assert(_entityToIndex[entity] != null)

        val removedEntityIndex = _entityToIndex[entity]
        val indexOfLastElement = _indexCounter - 1
        _componentArray[removedEntityIndex!!] = _componentArray[indexOfLastElement]

        val entityOfLastElement = _indexToEntity[indexOfLastElement]
        _entityToIndex[entityOfLastElement!!] = removedEntityIndex
        _indexToEntity[removedEntityIndex] = entityOfLastElement

        _entityToIndex.remove(entity)
        _indexToEntity.remove(indexOfLastElement)
        _componentArray.removeAt(indexOfLastElement)
        _indexCounter--
}

    fun getData(entity: Entity) : T {
        assert(_entityToIndex[entity] != null)
        return _componentArray[_entityToIndex[entity]!!]
    }

    fun getDataUnsafe(entity: Entity) : T? {
        val index = _entityToIndex[entity] ?: return null
        return _componentArray[index]
    }
    override fun entityDestroyed(entity: Entity) {
        if (_entityToIndex[entity] != null) {
            removeData(entity)
        }
    }

}

class ComponentManager {
    private val _componentArray = HashMap<KClass<out IComponent>, ComponentArray<out IComponent>>()
    private val _componentTypes = HashMap<KClass<out IComponent>, ComponentType>()
    private var _nextComponentType = 0

    internal fun _getComponentArrayDynamic(component: KClass<out IComponent>) : ComponentArray<out IComponent> {
        assert(_componentArray[component] != null)
        return _componentArray[component]!!
    }

    internal inline fun <reified T: IComponent> _getComponentArray() : ComponentArray<T> {
        assert(_componentArray[T::class] != null)
        return _componentArray[T::class]!! as ComponentArray<T>
    }

    internal inline fun <reified T : IComponent> registerComponent() {
        // checks that this component has not been registered yet
        assert(_componentArray[T::class] == null)
        assert(_componentTypes[T::class] == null)
        assert(_componentTypes.size < maxComponentTypes)

        _componentArray[T::class] = ComponentArray<T>()
        _componentTypes[T::class] = _nextComponentType
        _nextComponentType++
    }

    internal inline fun <reified T: IComponent> addComponent(entity: Entity, component: T) {
        val componentArray = _getComponentArray<T>()
        componentArray.insertData(entity, component)
    }

    internal fun addComponentDynamic(entity: Entity, component: IComponent) {
        _componentArray[component::class]!!.insertData(entity, component)
    }

    internal inline fun <reified T: IComponent> removeComponent(entity: Entity) {
        val componentArray = _getComponentArray<T>()
        componentArray.removeData(entity)
    }

    internal inline fun <reified T: IComponent> getComponent(entity: Entity) : T {
        return _getComponentArray<T>().getData(entity)
    }

    internal fun entityDestroyed(entity: Entity) {
        _componentArray.forEach { k, v -> v.entityDestroyed(entity) }
    }

    internal inline fun <reified T: IComponent> getComponentType() : ComponentType {
        assert(_componentTypes[T::class] != null)
        return _componentTypes[T::class]!!
    }

    fun getComponentDynamic(entity: Entity, component: KClass<out IComponent>) : IComponent {
        return _getComponentArrayDynamic(component).getData(entity)
    }

    fun getComponentDynamicUnsafe(entity: Entity, component: KClass<out IComponent>) : IComponent? {
        return _getComponentArrayDynamic(component).getDataUnsafe(entity)
    }

    fun registeredComponents() : MutableSet<KClass<out IComponent>> = _componentArray.keys

}
