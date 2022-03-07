package systems

import components.TimerComponent
import core.Entity
import core.Instance
import core.System

class TimerSystem : System() {
    override fun initializeLogic(vararg arg: Any): Boolean {
        return true
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        val entitiesToDestroy = ArrayList<Entity>()
        entities.forEach {
            val timerComponent = instance.getComponent<TimerComponent>(it)
            timerComponent.currentTime -= delta
            if (timerComponent.currentTime <= 0f)
                entitiesToDestroy.add(it)
        }
        entitiesToDestroy.forEach {
            instance.destroyEntity(it)
        }
    }

    override fun onEntityAdded(entity: Entity) {
    }

    override fun onEntityRemoved(entity: Entity) {
    }
}