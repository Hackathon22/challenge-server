package systems

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import components.DynamicComponent
import components.TransformComponent
import core.Entity
import core.Instance
import core.System
import core.Vec3F
import java.util.*
import kotlin.collections.HashMap

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
            dynamicComponent.speed.y += dynamicComponent.gravity * delta
            // speed modifies position
            transformComponent.pos += dynamicComponent.speed * delta
        }
    }

    override fun onEntityAdded(entity: Entity) {
    }

    override fun onEntityRemoved(entity: Entity) {
    }
}

class CollisionSystem : System() {

    private val _entityPositions = HashMap<Entity, Vec3F>()

    private val _addedEntities = LinkedList<Entity>()
    private val _removedEntities = LinkedList<Entity>()

    private val _world = World(Vector2(0.0f, 0.0f), true)

    override fun initializeLogic(vararg arg: Any): Boolean {
        return true
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        _addedEntities.forEach {
            // adds the entity entry in the position registry
            val transformComponent = instance.getComponent<TransformComponent>(it)
            _entityPositions[it] = transformComponent.pos
        }

        _removedEntities.forEach {
            _entityPositions.remove(it)
        }

        _addedEntities.clear()
        _removedEntities.clear()
    }

    override fun onEntityAdded(entity: Entity) {
        _addedEntities.add(entity)
    }

    override fun onEntityRemoved(entity: Entity) {
        _removedEntities.add(entity)
    }
}