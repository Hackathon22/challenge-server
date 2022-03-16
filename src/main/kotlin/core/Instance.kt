package core

import kotlin.reflect.KClass

class Instance {

    private val _componentManager = ComponentManager()
    private val _entityManager = EntityManager()
    private val _systemManager = SystemManager()


    fun createEntity(): Entity {
        return _entityManager.createEntity()
    }

    fun destroyEntity(entity: Entity) {
        _entityManager.destroyEntity(entity)
        _componentManager.entityDestroyed(entity)
        _systemManager.entityDestroyed(entity)
    }

    fun getAllEntities() : ArrayList<Entity> {
        return _entityManager.allEntities()
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

        val signature = _entityManager.getSignature(entity)
        signature.set(_componentManager.getComponentTypeDynamic(component::class), true)
        _entityManager.setSignature(entity, signature)
        _systemManager.entitySignatureChanged(entity, signature)
    }

    internal inline fun <reified T : IComponent> removeComponent(entity: Entity) {
        _componentManager.removeComponent<T>(entity)

        val signature = _entityManager.getSignature(entity)
        signature.set(_componentManager.getComponentType<T>(), false)
        _entityManager.setSignature(entity, signature)

        _systemManager.entitySignatureChanged(entity, signature)
    }

    internal fun getComponentDynamic(entity: Entity, component: KClass<out IComponent>) : IComponent {
        return _componentManager.getComponentDynamic(entity, component)
    }

    internal fun getComponentDynamicUnsafe(entity: Entity, component: KClass<out IComponent>) : IComponent? {
        return _componentManager.getComponentDynamicUnsafe(entity, component)
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

    fun getAllComponents(entity: Entity) : ArrayList<IComponent> {
        val components = ArrayList<IComponent>()
        for (componentClass in _componentManager.registeredComponents()) {
            val component = _componentManager.getComponentDynamicUnsafe(entity, componentClass)
            if (component != null) components.add(component)
        }
        return components
    }

    internal inline fun <reified T: System> getSystem() : T {
        return _systemManager.getSystem()
    }

}
