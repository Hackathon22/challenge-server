package systems

import core.Entity
import core.Instance
import core.System

class StateSystem : System() {
    override fun initializeLogic(vararg arg: Any): Boolean {
        return true
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        entities.forEach {
            stateLogic(instance, it)
        }
    }

    override fun onEntityAdded(entity: Entity) {
    }

    override fun onEntityRemoved(entity: Entity) {
    }

    private fun stateLogic(instance: Instance, entity: Entity) {
        
    }
}