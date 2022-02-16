package systems

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import components.BodyComponent
import components.DynamicComponent
import components.TransformComponent
import core.*
import java.util.*
import kotlin.collections.HashMap

class MovementSystem : System() {

    override fun initializeLogic(vararg arg: Any): Boolean {
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

    private val _entityBodies = HashMap<Entity, Body>()

    private val _addedEntities = LinkedList<Entity>()

    private val _removedEntities = LinkedList<Entity>()

    private val _world = World(Vector2(0.0f, 0.0f), false)

    override fun initializeLogic(vararg arg: Any): Boolean {
        try {
            val instance = arg[0] as Instance
            val contactListener = object : ContactListener {
                override fun beginContact(contact: Contact) {
                    val bodyA = contact.fixtureA.body
                    val bodyB = contact.fixtureB.body
                    // get entities
                    val bodyToEntities = _entityBodies.entries.associate { (k, v) -> v to k }
                    val entityA = bodyToEntities[bodyA]
                    val entityB = bodyToEntities[bodyB]

                    if (entityA != null && entityB != null) {
                        println("Detected contact between entity: $entityA and entity: $entityB")
                        val transformComponentA = instance.getComponent<TransformComponent>(entityA)
                        transformComponentA.pos = _entityPositions[entityA]!!
                        val dynamicComponentA = instance.getComponent<DynamicComponent>(entityA)
                        dynamicComponentA.speed.x = 0f
                        dynamicComponentA.speed.y = 0f

                        val transformComponentB = instance.getComponent<TransformComponent>(entityB)
                        transformComponentB.pos = _entityPositions[entityB]!!
                        val dynamicComponentB = instance.getComponent<DynamicComponent>(entityB)
                        dynamicComponentB.speed.x = 0f
                        dynamicComponentB.speed.y = 0f
                        // TODO make event
                    }
                }

                override fun endContact(contact: Contact) {
                }

                override fun preSolve(contact: Contact, oldManifold: Manifold?) {
                }

                override fun postSolve(contact: Contact, impulse: ContactImpulse?) {
                }
            }
            _world.setContactListener(contactListener)
            return true
        }
        catch (exc: TypeCastException) {
            return false
        }
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        _removedEntities.forEach {
            val body = _entityBodies[it]
            if (body != null)
                _world.destroyBody(_entityBodies[it])
            _entityPositions.remove(it)
            _entityBodies.remove(it)
        }

        _addedEntities.forEach {
            // adds the entity entry in the position registry
            val transformComponent = instance.getComponent<TransformComponent>(it)
            _entityPositions[it] = transformComponent.pos

            // gets the hitbox from the body component
            val bodyComponent = instance.getComponent<BodyComponent>(it)
            val width = bodyComponent.width
            val height = bodyComponent.height

            // box2d body, defines the object it the world
            val bodyDef = BodyDef()
            bodyDef.type = BodyDef.BodyType.DynamicBody
            bodyDef.fixedRotation = true
            bodyDef.position.set(Vector2(transformComponent.pos.x, transformComponent.pos.y))

            // box2d fixture, defines the collision box
            val fixDef = FixtureDef()

            // creates a rectangle around the body, the x, y position is on the bottom left
            val shape = PolygonShape()
            shape.setAsBox(width / 2.0f, height / 2.0f)

            fixDef.shape = shape
            fixDef.density = 1.0f
            // adds the body and it's fixture to the world
            val body = _world.createBody(bodyDef).createFixture(fixDef).body

            _entityBodies[it] = body
        }
        _addedEntities.clear()
        _removedEntities.clear()

        // detects collisions, rolling back the current positions
        _world.step(delta, 6, 2)

        // saves the entity position
        entities.forEach {
            // sets the body position in the world
            val transformComponent = instance.getComponent<TransformComponent>(it)
            _entityBodies[it]?.setTransform(
                Vector2(
                    transformComponent.pos.x,
                    transformComponent.pos.y
                ), transformComponent.rot.z
            )
        }

        entities.forEach {
            val transformComponent = instance.getComponent<TransformComponent>(it)
            _entityPositions[it] = transformComponent.pos
        }
    }

    override fun onEntityAdded(entity: Entity) {
        _addedEntities.add(entity)
    }

    override fun onEntityRemoved(entity: Entity) {
        _removedEntities.add(entity)
    }
}