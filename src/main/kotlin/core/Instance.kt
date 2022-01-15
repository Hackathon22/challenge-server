package core

import parser.ComponentMap
import kotlin.reflect.KClass

class Instance {

    private val _componentManager = ComponentManager()
    private val _entityManager = EntityManager()
    private val _systemManager = SystemManager()
    private val entities = ArrayList<Entity>()

    private val deletedEntities = ArrayList<Entity>()
    private val addedEntities = ArrayList<Entity>()

    fun createEntity(): Entity {
        val entity = _entityManager.createEntity()
        entities.add(entity)
        return entity
    }

    fun destroyEntity(entity: Entity) {
        _entityManager.destroyEntity(entity)
        _componentManager.entityDestroyed(entity)
        _systemManager.entityDestroyed(entity)
        entities.remove(entity)
    }

    internal inline fun <reified T : IComponent> registerComponent() {
        _componentManager.registerComponent<T>()
    }

    internal inline fun <reified T : IComponent> addComponent(entity: Entity, component: T) {
        _componentManager.addComponent(entity, component)

        val signature = _entityManager.getSignature(entity)
        signature.set(_componentManager.getComponentType<T>(), true)
        _entityManager.setSignature(entity, signature)

        _systemManager.entitySignatureChanged(entity, signature)
    }

    internal fun addComponentDynamic(entity: Entity, component: IComponent) {
        _componentManager.addComponentDynamic(entity, component)
    }

    internal inline fun <reified T : IComponent> removeComponent(entity: Entity, component: T) {
        _componentManager.removeComponent<T>(entity)

        val signature = _entityManager.getSignature(entity)
        signature.set(_componentManager.getComponentType<T>(), false)
        _entityManager.setSignature(entity, signature)

        _systemManager.entitySignatureChanged(entity, signature)
    }

    internal fun getComponentDynamic(entity: Entity, component: KClass<out IComponent>) : IComponent {
        return _componentManager.getComponentDynamic(entity, component)
    }

    internal inline fun <reified T : IComponent> getComponent(entity: Entity) : T {
        return _componentManager.getComponent(entity)
    }

    internal inline fun <reified T : IComponent> getComponentType() : ComponentType {
        return _componentManager.getComponentType<T>()
    }

    internal inline fun <reified T : System> registerSystem() : System {
        return _systemManager.registerSystem<T>()
    }

    internal inline fun <reified T : System> setSystemSignature(signature: Signature) {
        _systemManager.setSignature<T>(signature)
    }

    fun createComponentMap() : ComponentMap {
        val componentMap = ComponentMap()
        for (entity in 0 until entities.size) {
            componentMap[entity] = ArrayList()
            for (componentClass in _componentManager.registeredComponents()) {
                val component = _componentManager.getComponentDynamicUnsafe(entity, componentClass)
                if (component != null) componentMap[entity]!!.add(component)
            }
        }
        return componentMap
    }

    fun loadComponentMap(componentMap : ComponentMap) {
        componentMap.forEach { (_, components) ->
            val entity = createEntity()
            components.forEach{
                addComponentDynamic(entity, it)
            }
        }
    }

}
