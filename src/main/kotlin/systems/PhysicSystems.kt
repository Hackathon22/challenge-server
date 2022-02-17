package systems

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import components.BodyComponent
import components.DynamicComponent
import components.TransformComponent
import core.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.PI

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

    private val _entitiesToBodies = HashMap<Entity, Body>()
    private val _bodiesToEntities = HashMap<Body, Entity>()

    private val _entityPositions = HashMap<Entity, Vec3F>()

    private val _addedEntities = LinkedList<Entity>()

    private val _removedEntities = LinkedList<Entity>()

    private val _world = World(Vector2(0.0f, 0.0f), false)

    override fun initializeLogic(vararg arg: Any): Boolean {
        try {
            val instance = arg[0] as Instance
            val contactListener = object : ContactListener {
                override fun beginContact(contact: Contact) {
                    val entityA = _bodiesToEntities[contact.fixtureA.body]
                    val entityB = _bodiesToEntities[contact.fixtureB.body]

                    if (entityA != null && entityB != null) {
                        println("Detected contact between entity: $entityA and entity: $entityB")

                        // rolling back entity position to last non colliding position
                        val transformComponentA = instance.getComponent<TransformComponent>(entityA)
                        transformComponentA.pos = _entityPositions[entityA]!!
                        val transformComponentB = instance.getComponent<TransformComponent>(entityB)
                        transformComponentB.pos = _entityPositions[entityB]!!

                        // notifies listening systems
                        notifyObservers(CollisionEvent(entityA, entityB, 0.0f), instance) // todo remove placeholder angle
                    }
                }

                override fun endContact(contact: Contact) {
                    val entityA = _bodiesToEntities[contact.fixtureA.body]
                    val entityB = _bodiesToEntities[contact.fixtureB.body]
                    if (entityA != null && entityB != null) {
                        println("End of contact between entity: $entityA and entity: $entityB")
                    }
                }

                override fun preSolve(contact: Contact, oldManifold: Manifold?) {
                }

                override fun postSolve(contact: Contact, impulse: ContactImpulse?) {
                }
            }
            _world.setContactListener(contactListener)
            return true
        } catch (exc: TypeCastException) {
            return false
        }
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        _removedEntities.forEach {
            val body = _entitiesToBodies[it]
            if (body != null)
                _world.destroyBody(_entitiesToBodies[it])
            _entitiesToBodies.remove(it)
            _bodiesToEntities.remove(body)
        }

        _addedEntities.forEach {
            // adds the entity entry in the position registry
            val transformComponent = instance.getComponent<TransformComponent>(it)
            _entityPositions[it] = transformComponent.pos

            // gets the hitbox from the body component
            val bodyComponent = instance.getComponent<BodyComponent>(it)
            val width = bodyComponent.width
            val height = bodyComponent.height

            // box2d body, defines the object to the world
            val bodyDef = BodyDef()
            bodyDef.type = BodyDef.BodyType.DynamicBody
            bodyDef.fixedRotation = true

            // box2d fixture, defines the collision box
            val fixDef = FixtureDef()

            // creates a rectangle around the body, the x, y position is on the bottom left
            val shape = PolygonShape()
            shape.setAsBox(width / 2.0f, height / 2.0f)

            fixDef.shape = shape
            fixDef.isSensor = true // only behaves as a sensor (no knockback on collision)
            // adds the body and it's fixture to the world
            val body = _world.createBody(bodyDef).createFixture(fixDef).body

            _entitiesToBodies[it] = body
            _bodiesToEntities[body] = it
        }
        _addedEntities.clear()
        _removedEntities.clear()

        // saves the entity position
        entities.forEach {
            // sets the body position in the world
            val transformComponent = instance.getComponent<TransformComponent>(it)
            _entitiesToBodies[it]?.setTransform(
                Vector2(
                    transformComponent.pos.x,
                    transformComponent.pos.y
                ), transformComponent.rot.z * (PI.toFloat() / 180f)
            )
        }

        // detects collisions, rolling back the current positions
        _world.step(delta, 1, 1)

        entities.forEach {
            // saves the last transform
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