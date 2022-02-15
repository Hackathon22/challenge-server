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

    private val _world = World(Vector2(0.0f, 0.0f), true)

    override fun initializeLogic(vararg arg: Any): Boolean {
        val contactListener = object : ContactListener {
            override fun beginContact(contact: Contact) {
                val bodyA = contact.fixtureA.body
                val bodyB = contact.fixtureB.body
                // get entities
                val bodyToEntities = _entityBodies.entries.associate { (k, v) -> v to k }
                val entityA = bodyToEntities[bodyA]
                val entityB = bodyToEntities[bodyB]

                println("Detected contact between entity: $entityA and entity: $entityB")
                // TODO make event
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

    override fun updateLogic(instance: Instance, delta: Float) {
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
            bodyDef.type = BodyDef.BodyType.StaticBody

            // box2d fixture, defines the collision box
            val fixDef = FixtureDef()

            // creates a rectangle around the body, the x, y position is on the bottom left
            val shape = PolygonShape()
            val collisionBox = floatArrayOf(0.0f, 0.0f, 0.0f, height, width, height, width, 0.0f)
            shape.set(collisionBox)
            fixDef.shape = shape

            // adds the body and it's fixture to the world
            val body = _world.createBody(bodyDef)
            body.createFixture(fixDef)
            _entityBodies[it] = body
        }

        _removedEntities.forEach {
            val body = _entityBodies[it]
            if (body != null)
                _world.destroyBody(_entityBodies[it])
            _entityPositions.remove(it)
            _entityBodies.remove(it)
        }

        // sets the entity position
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
        _world.step(delta, 1, 1)

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