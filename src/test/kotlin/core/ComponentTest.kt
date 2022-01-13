package core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.lang.AssertionError

internal class ComponentTest {

    data class TransformComponent(var pos: Vec3 = Vec3(0.0f, 0.0f, 0.0f),
                                  var rot: Vec3 = Vec3(0.0f, 0.0f, 0.0f),
                                  var scale: Vec3 = Vec3(1.0f, 1.0f, 1.0f)
    ) : IComponent

    data class DynamicComponent(var speed: Vec3 = Vec3(0.0f, 0.0f, 0.0f),
                                var acceleration: Vec3 = Vec3(0.0f, 0.0f, 0.0f)
    ) : IComponent

    @Test
    fun testRegisterComponent() {
        val componentManager = ComponentManager()
        assertDoesNotThrow { componentManager.registerComponent<TransformComponent>() }

        var transformType : ComponentType? = null
        var dynamicType: ComponentType? = null

        assertDoesNotThrow { transformType = componentManager.getComponentType<TransformComponent>() }

        assertDoesNotThrow { componentManager.registerComponent<DynamicComponent>() }

        assertDoesNotThrow { dynamicType = componentManager.getComponentType<DynamicComponent>() }

        assert(transformType != dynamicType)
    }

    @Test
    fun testAddComponent() {
        val componentManager = ComponentManager()
        val entityManager = EntityManager()

        val entity = entityManager.createEntity()
        val component = TransformComponent()
        component.pos.x = 1.0
        component.rot.y = 2.0
        component.scale.z = 3.0

        componentManager.registerComponent<TransformComponent>()
        componentManager.addComponent(entity, component)

        val retrievedComponent = componentManager.getComponent<TransformComponent>(entity)

        assert(component.pos.x == retrievedComponent.pos.x)
        assert(component.rot.y == retrievedComponent.rot.y)
        assert(component.scale.z == retrievedComponent.scale.z)
    }

    @Test
    fun testAddUnregisteredComponent() {
        val componentManager = ComponentManager()
        val entityManager = EntityManager()

        val entity = entityManager.createEntity()
        val component = DynamicComponent()

        // register the wrong component
        componentManager.registerComponent<TransformComponent>()
        assertThrows<AssertionError> { componentManager.addComponent(entity, component) }
    }

    @Test
    fun testRemoveComponent() {
        val componentManager = ComponentManager()
        val entityManager = EntityManager()

        val entity = entityManager.createEntity()
        val component = TransformComponent()

        componentManager.registerComponent<TransformComponent>()

        // adds the component for the fist time
        componentManager.addComponent(entity, component)

        // removes the component
        componentManager.removeComponent<TransformComponent>(entity)

        assertThrows<AssertionError> { componentManager.getComponent<TransformComponent>(entity) }

        // puts the component back
        componentManager.addComponent(entity, component)

        assertDoesNotThrow { componentManager.getComponent<TransformComponent>(entity) }
    }

    @Test
    fun testComponentTypes() {
        val componentManager = ComponentManager()
        componentManager.registerComponent<TransformComponent>()
        componentManager.registerComponent<DynamicComponent>()

        val transformType = componentManager.getComponentType<TransformComponent>()
        val dynamicType = componentManager.getComponentType<DynamicComponent>()

        assert(transformType == 0)
        assert(dynamicType == 1)

        assertThrows<AssertionError> { componentManager.registerComponent<TransformComponent>() }
    }

    @Test
    fun testEntityDestroyed() {
        val componentManager = ComponentManager()
        val entityManager = EntityManager()

        componentManager.registerComponent<TransformComponent>()

        val entity = entityManager.createEntity()
        val component = TransformComponent()

        componentManager.addComponent(entity, component)

        componentManager.entityDestroyed(entity)

        assertThrows<AssertionError> { componentManager.getComponent<TransformComponent>(entity) }
    }

}
