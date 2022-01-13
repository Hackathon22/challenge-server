package core

import org.junit.jupiter.api.Test
import javax.swing.text.StyledEditorKit

internal class SystemTest {
    data class TransformComponent(var pos: Vec3 = Vec3(0.0f, 0.0f, 0.0f),
                                  var rot: Vec3 = Vec3(0.0f, 0.0f, 0.0f),
                                  var scale: Vec3 = Vec3(1.0f, 1.0f, 1.0f)
    ) : IComponent

    data class DynamicComponent(var speed: Vec3 = Vec3(0.0f, 0.0f, 0.0f),
                                var acceleration: Vec3 = Vec3(0.0f, 0.0f, 0.0f)
    ) : IComponent

    class MovementSystem : System() {

        override fun initializeLogic(vararg arg: Any) : Boolean {
            return true
        }

        override fun updateLogic(instance: Instance, delta: Float) {
            entities.forEach {
                val transformComponent = instance.getComponent<TransformComponent>(it)
                val dynamicComponent = instance.getComponent<DynamicComponent>(it)

                // acceleration modifies speed
                dynamicComponent.speed += dynamicComponent.acceleration * delta
                // speed modifies position
                transformComponent.pos += dynamicComponent.speed * delta
            }
        }
    }

    @Test
    fun testRegisterSystem() {
        val instance = Instance()

        instance.registerComponent<TransformComponent>()
        instance.registerComponent<DynamicComponent>()

        val movementSystem = instance.registerSystem<MovementSystem>()

        // creates a signature, which gives all the components needed by a system
        val signature = Signature(maxComponentTypes)
        signature.set(instance.getComponentType<TransformComponent>())
        signature.set(instance.getComponentType<DynamicComponent>())
        instance.setSystemSignature<MovementSystem>(signature)

        // creates one instance that is a dynamic moving object and one that is a static object
        val entity = instance.createEntity()
        val entity2 = instance.createEntity()

        val entityTransform = TransformComponent(Vec3(1.0, 0.0, 0.0))
        val entityDynamic = DynamicComponent(Vec3(-1.0, 0.0, 1.0), Vec3(0.0, 1.0, 0.0))

        val entity2Transform = TransformComponent(Vec3(0.0, 0.0, 0.0))

        instance.addComponent(entity, entityTransform)
        instance.addComponent(entity, entityDynamic)
        instance.addComponent(entity2, entity2Transform)

        val deltaTime = 1.0f
        movementSystem.update(instance, deltaTime)

        val entityTransformAfter = instance.getComponent<TransformComponent>(entity)
        println(entityTransformAfter)
        assert(entityTransformAfter.pos.x == 0.0)
        assert(entityTransformAfter.pos.y == 1.0)
        assert(entityTransformAfter.pos.z == 1.0)
    }
}