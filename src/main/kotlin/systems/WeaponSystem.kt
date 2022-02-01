package systems

import core.Entity
import core.Instance
import core.System

class WeaponSystem : System() {

    override fun initializeLogic(vararg arg: Any): Boolean {
        return true
    }

    override fun updateLogic(instance: Instance, delta: Float) {
    }

    override fun onEntityAdded(entity: Entity) {
    }

    override fun onEntityRemoved(entity: Entity) {
    }

    fun shoot(instance: Instance, entity: Entity) : Float {
        // creates a new bullet
        // TODO create bullet and remove this placeholder
        return 2.0f
    }

}