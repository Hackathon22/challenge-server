package systems

import components.DynamicComponent
import components.TransformComponent
import core.Instance
import core.System

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
